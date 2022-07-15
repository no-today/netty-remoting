package io.github.notoday.netty.remoting.common;

/**
 * @author no-today
 * @date 2022/06/02 15:27
 */
public class RemotingSysResponseCode {

    public static final int REQUEST_FAILED = -1;
    public static final int SUCCESS = 0;
    public static final int SYSTEM_ERROR = 1;
    public static final int SYSTEM_BUSY = 2;
    public static final int COMMAND_NOT_AVAILABLE_NOW = 3;
    public static final int REQUEST_CODE_NOT_SUPPORTED = 4;
    public static final int UNAUTHORIZED = 5;
}