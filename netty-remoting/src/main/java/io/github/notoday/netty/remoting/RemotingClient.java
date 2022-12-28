package io.github.notoday.netty.remoting;

import io.github.notoday.netty.remoting.core.ResultCallback;
import io.github.notoday.netty.remoting.exception.RemotingConnectException;
import io.github.notoday.netty.remoting.exception.RemotingSendRequestException;
import io.github.notoday.netty.remoting.exception.RemotingTimeoutException;
import io.github.notoday.netty.remoting.protocol.RemotingCommand;

/**
 * @author no-today
 * @date 2022/06/29 09:55
 */
public interface RemotingClient extends RemotingProcessable {

    /**
     * 登陆(连接)
     *
     * @param login    用户标识
     * @param token    身份密文
     * @param callback 结果回调
     */
    void login(String login, String token, long timeoutMillis, ResultCallback<RemotingCommand> callback);

    default void login(String login, String token, ResultCallback<RemotingCommand> callback) {
        login(login, token, 3000, callback);
    }

    /**
     * 登出(断开连接)
     */
    void logout(ResultCallback<Void> callback);

    void shutdown();

    /**
     * 同步调用
     * <p>
     * 阻塞至返回响应 或 超时
     *
     * @param request       请求指令
     * @param timeoutMillis 超时时间
     * @return 响应指令
     */
    RemotingCommand invokeSync(RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException;

    /**
     * 异步调用
     * <p>
     * 异常会走错误回调:
     * - InterruptedException,
     * - RemotingConnectException,
     * - RemotingSendRequestException,
     * - RemotingTimeoutException
     *
     * @param request        请求指令
     * @param timeoutMillis  超时时间
     * @param resultCallback 结果回调
     */
    void invokeAsync(RemotingCommand request, long timeoutMillis, ResultCallback<RemotingCommand> resultCallback);

    /**
     * 单向调用(发送消息，但不需要响应)
     *
     * @param request        请求指令
     * @param timeoutMillis  发送超时时间
     * @param resultCallback 结果回调
     */
    void invokeOneway(RemotingCommand request, long timeoutMillis, ResultCallback<Void> resultCallback);
}
