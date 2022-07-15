package io.github.notoday.netty.remoting.exception;

/**
 * 消息发送失败异常
 *
 * @author no-today
 * @date 2022/05/31 08:51
 */
public class RemotingSendRequestException extends RemotingException {

    private static final long serialVersionUID = 8311892129099228037L;

    public RemotingSendRequestException(String message) {
        super(message);
    }

    public RemotingSendRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
