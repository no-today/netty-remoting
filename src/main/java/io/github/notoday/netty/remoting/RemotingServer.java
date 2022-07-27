package io.github.notoday.netty.remoting;

import io.github.notoday.netty.remoting.core.ResultCallback;
import io.github.notoday.netty.remoting.exception.RemotingConnectException;
import io.github.notoday.netty.remoting.exception.RemotingSendRequestException;
import io.github.notoday.netty.remoting.exception.RemotingTimeoutException;
import io.github.notoday.netty.remoting.protocol.RemotingCommand;

/**
 * @author no-today
 * @date 2022/06/29 10:07
 */
public interface RemotingServer extends RemotingProcessable {

    void start();

    void shutdown();

    /**
     * 同步调用
     * <p>
     * 阻塞至返回响应 或 超时
     *
     * @param login         目标对端
     * @param request       请求指令
     * @param timeoutMillis 超时时间
     * @return 响应指令
     */
    RemotingCommand invokeSync(String login, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException;

    /**
     * 异步调用: 异常会走错误回调
     *
     * @param login          目标对端
     * @param request        请求指令
     * @param timeoutMillis  超时时间
     * @param resultCallback 结果回调
     */
    void invokeAsync(String login, RemotingCommand request, long timeoutMillis, ResultCallback<RemotingCommand> resultCallback);

    /**
     * 单向调用(发送消息，但不需要响应)
     *
     * @param login          目标对端
     * @param request        请求指令
     * @param timeoutMillis  发送超时时间
     * @param resultCallback 结果回调
     */
    void invokeOneway(String login, RemotingCommand request, long timeoutMillis, ResultCallback<Void> resultCallback);

    /**
     * 检查指定对端是否连接了当前节点
     *
     * @param login 目标对端
     * @return 是否可以通信
     */
    boolean isConnected(String login);
}
