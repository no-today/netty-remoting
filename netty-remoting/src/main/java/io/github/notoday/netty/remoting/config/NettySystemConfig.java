package io.github.notoday.netty.remoting.config;

/**
 * @author no-today
 * @date 2022/06/30 11:16
 */
public class NettySystemConfig {

    // Socket
    private static final String NETTY_REMOTING_SOCKET_SNDBUF_SIZE = "netty.remoting.socket.sndbuf.size";
    private static final String NETTY_REMOTING_SOCKET_RCVBUF_SIZE = "netty.remoting.socket.rcvbuf.size";
    private static final String NETTY_REMOTING_SOCKET_BACKLOG = "netty.remoting.socket.backlog";

    private static final String NETTY_REMOTING_WRITE_BUFFER_HIGH_WATER_MARK_VALUE = "netty.remoting.write.buffer.high.water.mark";
    private static final String NETTY_REMOTING_WRITE_BUFFER_LOW_WATER_MARK_VALUE = "netty.remoting.write.buffer.low.water.mark";

    // Client
    private static final String NETTY_REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE = "netty.remoting.client.asyncSemaphoreValue";
    private static final String NETTY_REMOTING_CLIENT_ONEWAY_SEMAPHORE_VALUE = "netty.remoting.client.onewaySemaphoreValue";
    private static final String NETTY_REMOTING_CLIENT_WORKER_THREADS = "netty.remoting.client.workerThreads";
    private static final String NETTY_REMOTING_CLIENT_CONNECT_TIMEOUT = "netty.remoting.client.connectTimeout";
    private static final String NETTY_REMOTING_CLIENT_CHANNEL_MAX_IDLE_SECONDS = "netty.remoting.client.channelMaxIdleTimeSeconds";
    private static final String NETTY_REMOTING_CLIENT_CLOSE_SOCKET_IF_TIMEOUT = "netty.remoting.client.closeSocketIfTimeout";

    // ----------------------------------------------------------------------
    public static int socketSndbufSize = Integer.parseInt(System.getProperty(NETTY_REMOTING_SOCKET_SNDBUF_SIZE, "0"));
    public static int socketRcvbufSize = Integer.parseInt(System.getProperty(NETTY_REMOTING_SOCKET_RCVBUF_SIZE, "0"));
    public static int socketBacklog = Integer.parseInt(System.getProperty(NETTY_REMOTING_SOCKET_BACKLOG, "1024"));
    public static int writeBufferHighWaterMarkValue = Integer.parseInt(System.getProperty(NETTY_REMOTING_WRITE_BUFFER_HIGH_WATER_MARK_VALUE, "0"));
    public static int writeBufferLowWaterMarkValue = Integer.parseInt(System.getProperty(NETTY_REMOTING_WRITE_BUFFER_LOW_WATER_MARK_VALUE, "0"));

    public static int clientAsyncSemaphoreValue = Integer.parseInt(System.getProperty(NETTY_REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE, "65535"));
    public static int clientOnewaySemaphoreValue = Integer.parseInt(System.getProperty(NETTY_REMOTING_CLIENT_ONEWAY_SEMAPHORE_VALUE, "65535"));
    public static int clientWorkerThreads = Integer.parseInt(System.getProperty(NETTY_REMOTING_CLIENT_WORKER_THREADS, "4"));
    public static int clientConnectTimeout = Integer.parseInt(System.getProperty(NETTY_REMOTING_CLIENT_CONNECT_TIMEOUT, "3000"));
    public static int clientChannelMaxIdleSeconds = Integer.parseInt(System.getProperty(NETTY_REMOTING_CLIENT_CHANNEL_MAX_IDLE_SECONDS, "120"));
    public static boolean clientCloseSocketIfTimeout = Boolean.parseBoolean(System.getProperty(NETTY_REMOTING_CLIENT_CLOSE_SOCKET_IF_TIMEOUT, "true"));
}
