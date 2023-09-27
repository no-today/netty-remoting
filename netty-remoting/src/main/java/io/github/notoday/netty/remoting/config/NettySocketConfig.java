package io.github.notoday.netty.remoting.config;

import lombok.Getter;
import lombok.Setter;

/**
 * @author no-today
 * @date 2022/06/30 14:40
 */
@Getter
@Setter
public class NettySocketConfig {

    private int socketSndBufSize = NettySystemConfig.socketSndbufSize;
    private int SocketRcvBufSize = NettySystemConfig.socketRcvbufSize;
    private int writeBufferHighWaterMark = NettySystemConfig.writeBufferHighWaterMarkValue;
    private int writeBufferLowWaterMark = NettySystemConfig.writeBufferLowWaterMarkValue;
    private int socketBacklog = NettySystemConfig.socketBacklog;

    private boolean enableSSL = true;
}
