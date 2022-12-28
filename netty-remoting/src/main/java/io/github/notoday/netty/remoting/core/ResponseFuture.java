package io.github.notoday.netty.remoting.core;

import io.github.notoday.netty.remoting.common.RemotingSysResponseCode;
import io.github.notoday.netty.remoting.common.SemaphoreReleaseOnlyOnce;
import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 与请求匹配, 在未来某个时间点通知
 *
 * @author no-today
 * @date 2022/05/29 15:03
 */
public class ResponseFuture {

    private final Channel channel;
    private final int reqId;
    private final long timeoutMillis;
    private final long requestTimestamp = System.currentTimeMillis();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final ResultCallback<RemotingCommand> responseCallback;
    private final AtomicBoolean executeResponseCallbackOnlyOnce = new AtomicBoolean(false);
    private final SemaphoreReleaseOnlyOnce semaphoreReleaseOnlyOnce;
    private volatile long responseTimestamp;
    private volatile RemotingCommand responseCommand;
    private volatile boolean sendRequestOk = true;
    private volatile Throwable cause;

    public ResponseFuture(Channel channel, int reqId, long timeoutMillis, ResultCallback<RemotingCommand> responseCallback, SemaphoreReleaseOnlyOnce semaphoreReleaseOnlyOnce) {
        this.channel = channel;
        this.reqId = reqId;
        this.timeoutMillis = timeoutMillis;
        this.responseCallback = responseCallback;
        this.semaphoreReleaseOnlyOnce = semaphoreReleaseOnlyOnce;
    }

    public ResponseFuture(Channel channel, int reqId, long timeoutMillis) {
        this(channel, reqId, timeoutMillis, null, null);
    }

    public void putResponse(final RemotingCommand responseCommand) {
        this.responseTimestamp = System.currentTimeMillis();
        this.responseCommand = responseCommand;
        this.countDownLatch.countDown(); // 通知前完成赋值
    }

    public RemotingCommand waitResponse(final long timeoutMillis) throws InterruptedException {
        this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.responseCommand;
    }

    public Channel getChannel() {
        return channel;
    }

    public long getReqId() {
        return reqId;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public long getRequestTimestamp() {
        return requestTimestamp;
    }

    /**
     * RTT (RoundTripTime): 创建请求的时间 ~ 返回响应的时候
     * <p>
     * 耗时包括:
     * 1. RPCHooks.before
     * 2. Write to server(encode)
     * 3. Server read(decode)
     * 4. Server Handler request
     * 5. Server write(encode)
     * 6. Read from server(decode)
     *
     * @return -1 表示请求未发出或者没有响应
     */
    public long getRTT() {
        return Math.max(-1, responseTimestamp - requestTimestamp);
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public RemotingCommand getResponseCommand() {
        return responseCommand;
    }

    public void setResponseCommand(RemotingCommand responseCommand) {
        this.responseCommand = responseCommand;
    }

    public boolean isSendRequestOk() {
        return sendRequestOk;
    }

    public void setSendRequestOk(boolean sendRequestOk) {
        this.sendRequestOk = sendRequestOk;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public ResultCallback<RemotingCommand> getResponseCallback() {
        return responseCallback;
    }

    public void executeCallback() {
        if (this.executeResponseCallbackOnlyOnce.compareAndSet(false, true)) {
            if (this.responseCommand == null) {
                this.responseCallback.onFailure(new ErrorInfo(this.reqId, -2, "timeout", this.cause));
                return;
            }

            if (RemotingSysResponseCode.SUCCESS == this.responseCommand.getCode()) {
                this.responseCallback.onSuccess(this.responseCommand);
            } else {
                this.responseCallback.onFailure(new ErrorInfo(this.reqId, this.responseCommand.getCode(), this.responseCommand.getRemark(), this.cause));
            }
        }
    }

    public void releaseSemaphore() {
        if (null != this.semaphoreReleaseOnlyOnce) {
            this.semaphoreReleaseOnlyOnce.release();
        }
    }

    @Override
    public String toString() {
        return "ResponseFuture{" + "reqId=" + reqId +
                ", timeoutMillis=" + timeoutMillis +
                ", requestTimestamp=" + requestTimestamp +
                '}';
    }
}
