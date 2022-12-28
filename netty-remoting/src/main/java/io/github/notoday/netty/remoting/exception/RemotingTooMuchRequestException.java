package io.github.notoday.netty.remoting.exception;

/**
 * 并发请求过多
 *
 * @author no-today
 * @date 2022/05/31 09:18
 */
public class RemotingTooMuchRequestException extends RemotingException {

    private static final long serialVersionUID = 4748062240223465707L;

    public RemotingTooMuchRequestException(String message) {
        super(message);
    }
}
