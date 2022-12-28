package io.github.notoday.netty.remoting.exception;

/**
 * 连接不可用时抛出
 *
 * @author no-today
 * @date 2022/05/31 08:50
 */
public class RemotingConnectException extends RemotingException {

    private static final long serialVersionUID = 669609001502354652L;

    public RemotingConnectException(String message) {
        super(message);
    }

    public RemotingConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
