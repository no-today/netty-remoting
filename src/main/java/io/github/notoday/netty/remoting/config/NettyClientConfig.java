package io.github.notoday.netty.remoting.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @author no-today
 * @date 2022/06/06 11:03
 */
@Getter
@Setter
public class NettyClientConfig extends NettySocketConfig implements Cloneable {

    /**
     * 接入点
     */
    private String host = "127.0.0.1";

    /**
     * 接入点端口
     */
    private int port = 7879;

    private int callbackExecutorThreads = Runtime.getRuntime().availableProcessors();
    private int workerThreads = NettySystemConfig.clientWorkerThreads;
    private int asyncSemaphoreValue = NettySystemConfig.clientAsyncSemaphoreValue;
    private int onewaySemaphoreValue = NettySystemConfig.clientOnewaySemaphoreValue;
    private int connectTimeoutMillis = NettySystemConfig.clientConnectTimeout;

    private int channelMaxIdleSeconds = NettySystemConfig.clientChannelMaxIdleSeconds;
    private boolean closeSocketIfTimeout = NettySystemConfig.clientCloseSocketIfTimeout;

    @Override
    public NettyClientConfig clone() {
        try {
            return (NettyClientConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
