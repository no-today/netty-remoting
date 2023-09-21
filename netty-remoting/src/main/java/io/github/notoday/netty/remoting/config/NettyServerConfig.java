package io.github.notoday.netty.remoting.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @author no-today
 * @date 2022/06/06 14:45
 */
@Getter
@Setter
public class NettyServerConfig extends NettySocketConfig implements Cloneable {

    private int listenPort = 7879;

    private int selectorThreads = 3;
    private int workerThreads = 8;
    private int callbackExecutorThreads = 0;

    private int onewaySemaphoreValue = 256;
    private int asyncSemaphoreValue = 64;
    private int channelMaxIdleSeconds = 120;

    private boolean serverPooledByteBufAllocatorEnable = true;

    private boolean enableSSL = true;

    @Override
    public NettyServerConfig clone() {
        try {
            return (NettyServerConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
