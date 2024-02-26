package io.github.notoday.netty.remoting.core;

import io.github.notoday.netty.remoting.NettyRequestProcessor;
import io.github.notoday.netty.remoting.RPCHook;
import io.github.notoday.netty.remoting.common.ErrorInfo;
import io.github.notoday.netty.remoting.common.Pair;
import io.github.notoday.netty.remoting.common.RemotingSystemCode;
import io.github.notoday.netty.remoting.common.SemaphoreReleaseOnlyOnce;
import io.github.notoday.netty.remoting.exception.RemotingConnectException;
import io.github.notoday.netty.remoting.exception.RemotingSendRequestException;
import io.github.notoday.netty.remoting.exception.RemotingTimeoutException;
import io.github.notoday.netty.remoting.exception.RemotingTooMuchRequestException;
import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.github.notoday.netty.remoting.security.RemotingSecurityUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author no-today
 * @date 2022/05/31 15:08
 */
@Slf4j
public abstract class NettyRemotingAbstract {

    /**
     * 异步命令信号量, 控制异步调用的并发数量, 从而保护系统内存
     */
    protected final Semaphore semaphoreAsync;

    /**
     * 单向命令信号量, 控制单向调用的并发数量, 从而保护系统内存
     */
    protected final Semaphore semaphoreOneway;

    /**
     * 缓存所有正在进行(未响应)的请求
     */
    protected final ConcurrentMap<Integer /* request id */, ResponseFuture> responseTable = new ConcurrentHashMap<>(128);

    /**
     * 通过请求编码找到请求处理器
     */
    protected final Map<Integer /* request code */, Pair<NettyRequestProcessor, ExecutorService>> processorTable = new HashMap<>(32);

    /**
     * 默认请求处理器
     * 当根据请求编码找不到处理器时使用默认处理器处理请求
     */
    protected Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;

    /**
     * 自定义的 RPC Hooks
     */
    protected List<RPCHook> rpcHooks = new ArrayList<>();

    public NettyRemotingAbstract(int permitsAsync, int permitsOneway) {
        this.semaphoreAsync = new Semaphore(permitsAsync, true);
        this.semaphoreOneway = new Semaphore(permitsOneway, true);
    }

    /**
     * 从子类获取回调执行器
     * 如果为空或者任务已满, 交由当前线程执行回调
     */
    public abstract ExecutorService getCallbackExecutor();

    public void registerProcessor(final int requestCode, final ExecutorService executor, final NettyRequestProcessor processor) {
        this.processorTable.put(requestCode, new Pair<>(processor, executor));
    }

    public void registerDefaultProcessor(final ExecutorService executor, final NettyRequestProcessor processor) {
        this.defaultRequestProcessor = new Pair<>(processor, executor);
    }

    public void registerRPCHook(final RPCHook rpcHook) {
        if (rpcHook != null && !this.rpcHooks.contains(rpcHook)) {
            this.rpcHooks.add(rpcHook);
        }
    }

    protected void doBeforeRPCHooks(String login, RemotingCommand request) {
        if (!this.rpcHooks.isEmpty()) {
            for (RPCHook rpcHook : this.rpcHooks) {
                rpcHook.doBeforeRequest(login, request);
            }
        }
    }

    protected void doAfterRPCHooks(String login, RemotingCommand request, RemotingCommand response) {
        if (!this.rpcHooks.isEmpty()) {
            for (RPCHook rpcHook : this.rpcHooks) {
                rpcHook.doAfterResponse(login, request, response);
            }
        }
    }

    public void processMessageReceived(ChannelHandlerContext ctx, RemotingCommand cmd) throws Exception {
        if (cmd == null) return;
        if (cmd.isResponse()) {
            processResponseCommand(ctx, cmd);
        } else {
            processRequestCommand(ctx, cmd);
        }
    }

    public void processRequestCommand(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        Pair<NettyRequestProcessor, ExecutorService> pair = this.processorTable.getOrDefault(request.getCode(), this.defaultRequestProcessor);

        // 担心在 processor 里被篡改, 安全起见留个副本
        final int reqId = request.getReqId();
        final boolean oneway = request.isOneway();

        if (pair == null) {
            response(ctx, RemotingCommand.failure(reqId, RemotingSystemCode.REQUEST_CODE_NOT_SUPPORTED, "[REQUEST_CODE_NOT_SUPPORTED] request code " + request.getCode() + " not supported"));
            return;
        }

        NettyRequestProcessor requestProcessor = pair.getObj1();
        if (requestProcessor.rejectRequest()) {
            response(ctx, RemotingCommand.failure(reqId, RemotingSystemCode.COMMAND_NOT_AVAILABLE_NOW, "[COMMAND_UNAVAILABLE_NOW] this command is currently unavailable"));
            return;
        }

        RequestTask task = new RequestTask(() -> {
            try {
                String login = RemotingSecurityUtils.getCurrentLogin(ctx.channel());
                this.doBeforeRPCHooks(login, request);

                // 默认实现实际上还是同步
                requestProcessor.asyncProcessRequest(ctx, request, response -> {
                    NettyRemotingAbstract.this.doAfterRPCHooks(login, request, response);

                    if (!oneway && response != null) {
                        response.setReqId(reqId);
                        response.markResponseType();

                        response(ctx, response);
                    }
                });
            } catch (Throwable e) {
                log.error("process request exception", e);
                log.error("{}", request);

                // 单向消息不需要响应
                if (!oneway) {
                    response(ctx, RemotingCommand.failure(reqId, RemotingSystemCode.SYSTEM_ERROR, e.getMessage()));
                }
            }
        }, ctx.channel(), request);

        try {
            pair.getObj2().submit(task);
        } catch (RejectedExecutionException e) {
            // 10s print once log
            if (System.currentTimeMillis() % 10000 == 0) {
                log.warn("too many requests and system thread pool busy, RejectedExecutionException");
            }

            // 单向消息不需要响应
            if (!oneway) {
                response(ctx, RemotingCommand.failure(reqId, RemotingSystemCode.SYSTEM_BUSY, "[OVERLOAD] system busy, try later"));
            }
        }
    }

    private void response(ChannelHandlerContext ctx, RemotingCommand response) {
        ctx.writeAndFlush(response);
    }

    public void processResponseCommand(ChannelHandlerContext ctx, RemotingCommand response) throws Exception {
        int reqId = response.getReqId();
        ResponseFuture future = this.responseTable.remove(reqId);
        if (null != future) {
            future.putResponse(response);
            executionCallback(future);
            future.releaseSemaphore();
        } else {
            log.warn("receive response command, but not matched any request, reqId: {}", response.getReqId());
        }
    }

    /**
     * 执行响应回调
     * <p>
     * 先尝试异步执行, 若无法异步则在当前线程执行回调
     */
    protected void executionCallback(final ResponseFuture responseFuture) {
        if (null == responseFuture.getResponseCallback()) return;

        log.debug("execute callback, req: {}, RTT: {}ms", responseFuture.getReqId(), responseFuture.getRTT());

        boolean runInThisThread = false;
        ExecutorService executor = getCallbackExecutor();
        if (null != executor) {
            try {
                executor.submit(() -> {
                    try {
                        responseFuture.executeCallback();
                    } catch (Exception e) {
                        log.warn("execute callback in executor exception, and callback throw", e);
                    } finally {
                        responseFuture.releaseSemaphore();
                    }
                });
            } catch (Exception e) {
                runInThisThread = true;
                log.warn("execute callback in executor exception, maybe executor busy", e);
            }
        } else {
            runInThisThread = true;
        }

        if (runInThisThread) {
            try {
                responseFuture.executeCallback();
            } catch (Throwable e) {
                log.warn("executeCallback Exception", e);
            } finally {
                responseFuture.releaseSemaphore();
            }
        }
    }

    /**
     * 扫描已经超时的请求, 并进行回调通知
     */
    protected void scanResponseTable() {
        List<ResponseFuture> rfList = new LinkedList<>();
        Iterator<Map.Entry<Integer, ResponseFuture>> it = responseTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, ResponseFuture> next = it.next();
            ResponseFuture rep = next.getValue();

            if (rep.getRequestTimestamp() + rep.getTimeoutMillis() <= System.currentTimeMillis()) {
                rep.releaseSemaphore();
                it.remove();
                rfList.add(rep);

                log.warn("remove timeout request, {}", rep);
            }
        }

        for (ResponseFuture future : rfList) {
            executionCallback(future);
        }
    }

    protected RemotingCommand invokeSyncImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        ifChannelUnavailableThrowException(channel);

        int reqId = request.getReqId();
        ResponseFuture responseFuture = new ResponseFuture(channel, reqId, timeoutMillis);
        this.responseTable.put(reqId, responseFuture);

        String login = RemotingSecurityUtils.getCurrentLogin(channel);
        this.doBeforeRPCHooks(login, request);

        channel.writeAndFlush(request).addListener(future -> {
            if (future.isSuccess()) {
                responseFuture.setSendRequestOk(true);
            } else {
                responseFuture.setSendRequestOk(false);
                responseFuture.setCause(future.cause());
                responseFuture.putResponse(null);   // 触发闭锁
            }
        });

        try {
            // 同步请求, 阻塞等待响应
            RemotingCommand response = responseFuture.waitResponse(timeoutMillis);
            this.doAfterRPCHooks(login, request, response);

            if (null == response) {
                // 发送成功未响应则是对端处理超时
                if (responseFuture.isSendRequestOk()) {
                    throw new RemotingTimeoutException(timeoutMillis, responseFuture.getCause());
                } else {
                    throw new RemotingSendRequestException("failed to send request to channel", responseFuture.getCause());
                }
            }

            log.debug("return response, reqId: {}, RTT: {}ms", responseFuture.getReqId(), responseFuture.getRTT());

            return response;
        } finally {
            this.responseTable.remove(reqId);
        }
    }

    protected void invokeAsyncImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis, final ResultCallback<RemotingCommand> resultCallback) {
        try {
            ifChannelUnavailableThrowException(channel);

            if (!this.semaphoreAsync.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
                String info = String.format("invokeAsync tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreOnewayValue: %d", timeoutMillis, this.semaphoreAsync.getQueueLength(), this.semaphoreAsync.availablePermits());
                throw new RemotingTooMuchRequestException(info);
            }

            int reqId = request.getReqId();
            ResponseFuture responseFuture = new ResponseFuture(channel, reqId, timeoutMillis, resultCallback, new SemaphoreReleaseOnlyOnce(this.semaphoreAsync));
            this.responseTable.put(reqId, responseFuture);

            try {
                String login = RemotingSecurityUtils.getCurrentLogin(channel);
                this.doBeforeRPCHooks(login, request);

                channel.writeAndFlush(request).addListener(future -> {
                    if (future.isSuccess()) {
                        responseFuture.setSendRequestOk(true);
                    } else {
                        log.warn("send a request command to channel failed", future.cause());

                        this.responseTable.remove(reqId);
                        responseFuture.setSendRequestOk(false);
                        responseFuture.setCause(future.cause());
                        responseFuture.putResponse(null);
                        executionCallback(responseFuture);
                    }
                });
            } catch (Exception e) {
                String s = "write a request command to channel failed";
                log.warn(s, e);
                throw new RemotingSendRequestException(s, e);
            }

        } catch (Throwable e) {
            resultCallback.onFailure(new ErrorInfo(request.getReqId(), -1, e.getMessage(), e));
        }
    }

    protected void invokeOnewayImpl(final Channel channel, final RemotingCommand request, final long timeoutMillis, final ResultCallback<Void> resultCallback) {
        try {
            ifChannelUnavailableThrowException(channel);

            request.markOnewayRPC();
            if (!this.semaphoreOneway.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) {
                String info = String.format("invokeOnewayImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreOnewayValue: %d", timeoutMillis, this.semaphoreOneway.getQueueLength(), this.semaphoreOneway.availablePermits());
                throw new RemotingTooMuchRequestException(info);
            }

            int reqId = request.getReqId();
            ResponseFuture responseFuture = new ResponseFuture(channel, reqId, timeoutMillis, null, new SemaphoreReleaseOnlyOnce(this.semaphoreOneway));

            try {
                String login = RemotingSecurityUtils.getCurrentLogin(channel);
                this.doBeforeRPCHooks(login, request);

                channel.writeAndFlush(request).addListener(future -> {
                    responseFuture.releaseSemaphore();
                    if (!future.isSuccess()) {
                        String message = "send a request command to channel failed";
                        log.warn(message, future.cause());
                        resultCallback.onFailure(new ErrorInfo(reqId, -1, message, future.cause()));
                    } else {
                        resultCallback.onSuccess(null);
                    }
                });

            } catch (Exception e) {
                responseFuture.releaseSemaphore();
                String message = "write a request command to channel failed";
                log.warn(message, e);
                throw new RemotingSendRequestException(message, e);
            }
        } catch (Throwable e) {
            resultCallback.onFailure(new ErrorInfo(request.getReqId(), -1, e.getMessage(), e));
        }
    }

    private void ifChannelUnavailableThrowException(Channel channel) throws RemotingConnectException {
        if (null == channel || !channel.isActive()) {
            throw new RemotingConnectException("channel unavailable");
        }
    }
}
