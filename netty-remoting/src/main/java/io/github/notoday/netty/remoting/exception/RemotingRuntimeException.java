package io.github.notoday.netty.remoting.exception;

/**
 * @author no-today
 * @date 2023/09/22 11:15
 */
public class RemotingRuntimeException extends RuntimeException {

    public RemotingRuntimeException(String message) {
        super(message);
    }

    public RemotingRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
