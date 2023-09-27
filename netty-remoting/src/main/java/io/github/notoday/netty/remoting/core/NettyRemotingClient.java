package io.github.notoday.netty.remoting.core;

import io.github.notoday.netty.remoting.ChannelEventListener;
import io.github.notoday.netty.remoting.RemotingClient;
import io.github.notoday.netty.remoting.common.ErrorInfo;
import io.github.notoday.netty.remoting.common.RemotingSystemCode;
import io.github.notoday.netty.remoting.common.RemotingUtil;
import io.github.notoday.netty.remoting.config.NettyClientConfig;
import io.github.notoday.netty.remoting.exception.RemotingConnectException;
import io.github.notoday.netty.remoting.exception.RemotingSendRequestException;
import io.github.notoday.netty.remoting.exception.RemotingTimeoutException;
import io.github.notoday.netty.remoting.protocol.Any;
import io.github.notoday.netty.remoting.protocol.NettyDecoder;
import io.github.notoday.netty.remoting.protocol.NettyEncoder;
import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.github.notoday.netty.remoting.security.AuthenticationToken;
import io.github.notoday.netty.remoting.ssl.OpenSslProvider;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author no-today
 * @date 2022/06/05 10:26
 */
@Slf4j
public class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient {

    private final NettyClientConfig config;

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroupSelector;
    private final DefaultEventExecutorGroup defaultEventExecutorGroup;
    private final ExecutorService callbackExecutor;
    private final Timer timer = new Timer("ClientHouseKeepingService", true);
    private final ChannelEventListener channelEventListener;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private Channel channel;

    // sharable handlers
    private NettyDecoder protocDecoder;
    private NettyEncoder protocEncoder;

    private NettyClientConnectManageHandler nettyClientConnectManageHandler;
    private NettyClientHandler nettyClientHandler;

    public NettyRemotingClient(NettyClientConfig config, ChannelEventListener channelEventListener) {
        super(config.getAsyncSemaphoreValue(), config.getOnewaySemaphoreValue());
        this.config = config.clone();
        this.channelEventListener = channelEventListener;

        this.bootstrap = new Bootstrap();
        this.callbackExecutor = Executors.newFixedThreadPool(Math.max(4, config.getCallbackExecutorThreads()), RemotingUtil.newThreadFactory("NettyClientCallbackExecutor"));
        this.eventLoopGroupSelector = new NioEventLoopGroup(1, RemotingUtil.newThreadFactory("NettyClientSelector"));
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(config.getWorkerThreads(), RemotingUtil.newThreadFactory("NettyClientWorker"));
    }

    private void prepareSharableHandlers() {
        this.protocDecoder = new NettyDecoder();
        this.protocEncoder = new NettyEncoder();

        this.nettyClientConnectManageHandler = new NettyClientConnectManageHandler();
        this.nettyClientHandler = new NettyClientHandler();
    }

    private void setup() {
        if (this.initialized.compareAndSet(false, true)) {
            prepareSharableHandlers();

            Bootstrap handler = this.bootstrap.group(this.eventLoopGroupSelector)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, false)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.config.getConnectTimeoutMillis())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            if (config.isEnableSSL()) {
                                ch.pipeline().addLast(OpenSslProvider.client().newHandler(ch.alloc()));
                            }

                            ch.pipeline().addLast(NettyRemotingClient.this.defaultEventExecutorGroup,
                                    NettyRemotingClient.this.protocDecoder,
                                    NettyRemotingClient.this.protocEncoder,
                                    new IdleStateHandler(0, 0, NettyRemotingClient.this.config.getChannelMaxIdleSeconds()),
                                    NettyRemotingClient.this.nettyClientConnectManageHandler,
                                    NettyRemotingClient.this.nettyClientHandler
                            );
                        }
                    });

            if (this.config.getSocketSndBufSize() > 0) {
                log.info("client set SO_SNDBUF to {}", this.config.getSocketSndBufSize());
                handler.option(ChannelOption.SO_SNDBUF, this.config.getSocketSndBufSize());
            }
            if (this.config.getSocketRcvBufSize() > 0) {
                log.info("client set SO_RCVBUF to {}", this.config.getSocketRcvBufSize());
                handler.option(ChannelOption.SO_RCVBUF, this.config.getSocketRcvBufSize());
            }
            if (this.config.getWriteBufferLowWaterMark() > 0 && this.config.getWriteBufferHighWaterMark() > 0) {
                log.info("client set netty WRITE_BUFFER_WATER_MARK to {},{}", this.config.getWriteBufferLowWaterMark(), this.config.getWriteBufferHighWaterMark());
                handler.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(this.config.getWriteBufferLowWaterMark(), this.config.getWriteBufferHighWaterMark()));
            }

            this.timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        scanResponseTable();
                    } catch (Exception e) {
                        log.error("scanResponseTable exception", e);
                    }
                }
            }, 1000, 1000);
        }
    }

    @Override
    public void shutdown() {
        this.timer.cancel();
        if (this.channel != null) RemotingUtil.closeChannel(this.channel);

        this.eventLoopGroupSelector.shutdownGracefully();
        this.defaultEventExecutorGroup.shutdownGracefully();
        this.callbackExecutor.shutdown();
    }

    private Channel createChannel() throws RemotingConnectException {
        if (this.channel != null && this.channel.isActive()) return this.channel;

        try {
            return this.bootstrap.connect(config.getHost(), config.getPort()).sync().channel();
        } catch (InterruptedException e) {
            throw new RemotingConnectException("Failed to establish connection", e);
        }
    }

    @Override
    public void login(@NonNull String token, @NonNull String login, long timeoutMillis, ResultCallback<RemotingCommand> callback) {
        setup();

        try {
            this.channel = createChannel();
            RemotingCommand response = invokeSync(RemotingCommand.request(RemotingSystemCode.AUTHENTICATION,
                    Any.pack(new AuthenticationToken().setToken(token).setLogin(login))), timeoutMillis);

            if (response.success()) {
                callback.onSuccess(response);
            } else {
                callback.onFailure(new ErrorInfo(response.getReqId(), response.getCode(), response.getMessage()));
            }
        } catch (RemotingConnectException e) {
            callback.onFailure(new ErrorInfo(-1, RemotingSystemCode.REQUEST_FAILED, e));
        } catch (Exception e) {
            callback.onFailure(new ErrorInfo(-1, RemotingSystemCode.SYSTEM_ERROR, e));
        }
    }

    @Override
    public void logout(ResultCallback<Void> callback) {
        try {
            RemotingCommand response = invokeSync(RemotingCommand.request(RemotingSystemCode.AUTHENTICATION, null), 3000);
            if (response.success()) {
                callback.onSuccess(null);
            } else {
                callback.onFailure(new ErrorInfo(-1, response.getCode(), response.getMessage()));
            }
        } catch (Exception e) {
            callback.onFailure(new ErrorInfo(-1, RemotingSystemCode.REQUEST_FAILED, e.getMessage()));
        }
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return this.callbackExecutor;
    }

    @Override
    public RemotingCommand invokeSync(RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        return super.invokeSyncImpl(this.channel, request, timeoutMillis);
    }

    @Override
    public void invokeAsync(RemotingCommand request, long timeoutMillis, ResultCallback<RemotingCommand> resultCallback) {
        super.invokeAsyncImpl(this.channel, request, timeoutMillis, resultCallback);
    }

    @Override
    public void invokeOneway(RemotingCommand request, long timeoutMillis, ResultCallback<Void> resultCallback) {
        super.invokeOnewayImpl(this.channel, request, timeoutMillis, resultCallback);

    }

    @ChannelHandler.Sharable
    class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }

    @ChannelHandler.Sharable
    class NettyClientConnectManageHandler extends NettyAbstractConnectManageHandler {
        public NettyClientConnectManageHandler() {
            super("CLIENT", NettyRemotingClient.this.channelEventListener);
        }
    }
}
