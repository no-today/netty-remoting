package io.github.notoday.netty.remoting.exception;

/**
 * @author no-today
 * @date 2022/06/23 16:54
 */
public class AuthenticationException extends RemotingException {

    private static final long serialVersionUID = -4266105146986731692L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
