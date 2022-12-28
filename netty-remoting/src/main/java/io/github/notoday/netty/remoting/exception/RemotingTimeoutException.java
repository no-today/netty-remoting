package io.github.notoday.netty.remoting.exception;

/**
 * 对端处理超时异常
 *
 * @author no-today
 * @date 2022/05/31 08:50
 */
public class RemotingTimeoutException extends RemotingException {

    private static final long serialVersionUID = -2656759191392731401L;

    public RemotingTimeoutException(long timeoutMillis, Throwable cause) {
        super("timeout waiting for channel response, timeout millis is " + timeoutMillis, cause);
    }

    public RemotingTimeoutException(String message) {
        super(message);
    }
}
