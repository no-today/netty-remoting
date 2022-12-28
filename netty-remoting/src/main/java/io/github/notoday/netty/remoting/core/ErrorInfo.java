package io.github.notoday.netty.remoting.core;

import lombok.Getter;
import lombok.ToString;

/**
 * @author no-today
 * @date 2022/06/27 17:33
 */
@Getter
@ToString
public class ErrorInfo {

    private final int reqId;
    private final int errorCode;
    private final String errorDesc;
    private final Throwable cause;

    public ErrorInfo(int reqId, int errorCode, String errorDesc, Throwable cause) {
        this.reqId = reqId;
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
        this.cause = cause;
    }

    public ErrorInfo(int reqId, int errorCode, String errorDesc) {
        this(reqId, errorCode, errorDesc, null);
    }

    public ErrorInfo(int reqId, int errorCode, Throwable cause) {
        this(reqId, errorCode, null, cause);
    }

    public ErrorInfo(int reqId, int errorCode) {
        this(reqId, errorCode, null, null);
    }
}
