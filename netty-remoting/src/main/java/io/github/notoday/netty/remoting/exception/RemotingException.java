package io.github.notoday.netty.remoting.exception;

/**
 * 远程模块异常的基类
 *
 * @author no-today
 * @date 2022/05/30 19:57
 */
public class RemotingException extends Exception {

    private static final long serialVersionUID = 15096152096788952L;

    public RemotingException(String message) {
        super(message);
    }

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
