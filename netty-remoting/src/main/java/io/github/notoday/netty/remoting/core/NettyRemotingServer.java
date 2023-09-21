package io.github.notoday.netty.remoting.core;

import io.github.notoday.netty.remoting.ChannelEventListener;
import io.github.notoday.netty.remoting.RemotingServer;
import io.github.notoday.netty.remoting.common.RemotingUtil;
import io.github.notoday.netty.remoting.config.NettyServerConfig;
import io.github.notoday.netty.remoting.exception.RemotingConnectException;
import io.github.notoday.netty.remoting.exception.RemotingSendRequestException;
import io.github.notoday.netty.remoting.exception.RemotingTimeoutException;
import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.github.notoday.netty.remoting.protocol.protobuf.RemotingCommandProtobuf;
import io.github.notoday.netty.remoting.security.AuthenticationEvent;
import io.github.notoday.netty.remoting.security.Authenticator;
import io.github.notoday.netty.remoting.security.NettyAuthenticatorHandler;
import io.github.notoday.netty.remoting.security.RemotingSecurityUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author no-today
 * @date 2022/06/06 10:50
 */
@Slf4j
public class NettyRemotingServer extends NettyRemotingAbstract implements RemotingServer {

    private final NettyServerConfig config;

    private final ServerBootstrap serverBootstrap;
    private final EventLoopGroup eventLoopGroupBoss;
    private final EventLoopGroup eventLoopGroupSelector;
    private final DefaultEventExecutorGroup defaultEventExecutorGroup;

    private final ExecutorService callbackExecutor;

    private final Timer timer = new Timer("ServerHouseKeepingService", true);

    private final ChannelEventListener channelEventListener;

    private final Authenticator authenticator;
    private final ConcurrentHashMap<String, Channel> channelTable = new ConcurrentHashMap<>();

    // sharable handlers
    private ProtobufDecoder protobufDecoder;
    private ProtobufVarint32LengthFieldPrepender protobufVarint32LengthFieldPrepender;
    private ProtobufEncoder protobufEncoder;
    private NettyAuthenticatorHandler nettyAuthenticatorHandler;
    private NettyServerConnectManageHandler nettyServerConnectManageHandler;
    private NettyServerHandler nettyServerHandler;

    public NettyRemotingServer(NettyServerConfig config, ChannelEventListener channelEventListener, Authenticator authenticator) {
        super(config.getAsyncSemaphoreValue(), config.getOnewaySemaphoreValue());
        this.config = config.clone();
        this.channelEventListener = channelEventListener;
        this.authenticator = authenticator;

        this.serverBootstrap = new ServerBootstrap();
        this.callbackExecutor = Executors.newFixedThreadPool(Math.max(4, config.getCallbackExecutorThreads()), RemotingUtil.newThreadFactory("NettyServerCallbackExecutor"));
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(config.getWorkerThreads(), RemotingUtil.newThreadFactory("NettyServerCodecThread"));

        if (useEpoll()) {
            log.debug("use epoll");
            this.eventLoopGroupBoss = new EpollEventLoopGroup(1, RemotingUtil.newThreadFactory("NettyServerEpollBoss"));
            this.eventLoopGroupSelector = new EpollEventLoopGroup(config.getSelectorThreads(), RemotingUtil.newThreadFactory("NettyServerEpollSelector"));
        } else {
            this.eventLoopGroupBoss = new NioEventLoopGroup(1, RemotingUtil.newThreadFactory("NettyServerNioBoss"));
            this.eventLoopGroupSelector = new NioEventLoopGroup(config.getSelectorThreads(), RemotingUtil.newThreadFactory("NettyServerNioSelector"));
        }
    }

    private boolean useEpoll() {
        return RemotingUtil.isLinuxPlatform() && Epoll.isAvailable();
    }

    private void prepareSharableHandlers() {
        this.protobufDecoder = new ProtobufDecoder(RemotingCommandProtobuf.getDefaultInstance());

        this.protobufVarint32LengthFieldPrepender = new ProtobufVarint32LengthFieldPrepender();
        this.protobufEncoder = new ProtobufEncoder();

        this.nettyAuthenticatorHandler = new NettyAuthenticatorHandler(this.authenticator);
        this.nettyServerConnectManageHandler = new NettyServerConnectManageHandler();
        this.nettyServerHandler = new NettyServerHandler();
    }

    @Override
    public void start() {
        prepareSharableHandlers();

        ServerBootstrap handler = this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, this.config.getSocketBacklog()).option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .localAddress(new InetSocketAddress(this.config.getListenPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(NettyRemotingServer.this.defaultEventExecutorGroup,
                                new ProtobufVarint32FrameDecoder(),
                                NettyRemotingServer.this.protobufDecoder,
                                NettyRemotingServer.this.protobufVarint32LengthFieldPrepender,
                                NettyRemotingServer.this.protobufEncoder,
                                new IdleStateHandler(0, 0, NettyRemotingServer.this.config.getChannelMaxIdleSeconds()),
                                NettyRemotingServer.this.nettyAuthenticatorHandler,
                                NettyRemotingServer.this.nettyServerConnectManageHandler,
                                NettyRemotingServer.this.nettyServerHandler);
                    }
                });

        if (this.config.getSocketSndBufSize() > 0) {
            log.info("server set SO_SNDBUF to {}", this.config.getSocketSndBufSize());
            handler.option(ChannelOption.SO_SNDBUF, this.config.getSocketSndBufSize());
        }
        if (this.config.getSocketRcvBufSize() > 0) {
            log.info("server set SO_RCVBUF to {}", this.config.getSocketRcvBufSize());
            handler.option(ChannelOption.SO_RCVBUF, this.config.getSocketRcvBufSize());
        }
        if (this.config.getWriteBufferLowWaterMark() > 0 && this.config.getWriteBufferHighWaterMark() > 0) {
            log.info("server set netty WRITE_BUFFER_WATER_MARK to {},{}", this.config.getWriteBufferLowWaterMark(), this.config.getWriteBufferHighWaterMark());
            handler.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(this.config.getWriteBufferLowWaterMark(), this.config.getWriteBufferHighWaterMark()));
        }

        if (this.config.isServerPooledByteBufAllocatorEnable()) {
            handler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }

        try {
            this.serverBootstrap.bind().sync();
        } catch (InterruptedException e1) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e1);
        }

        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    scanResponseTable();
                } catch (Throwable e) {
                    log.error("scanResponseTable exception", e);
                }
            }
        }, 1000, 1000);
    }

    @Override
    public void shutdown() {
        this.timer.cancel();

        this.eventLoopGroupBoss.shutdownGracefully();
        this.eventLoopGroupSelector.shutdownGracefully();
        this.defaultEventExecutorGroup.shutdownGracefully();
        this.callbackExecutor.shutdown();
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return this.callbackExecutor;
    }

    @Override
    public RemotingCommand invokeSync(String login, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        return super.invokeSyncImpl(getChannel(login), request, timeoutMillis);
    }

    @Override
    public void invokeAsync(String login, RemotingCommand request, long timeoutMillis, ResultCallback<RemotingCommand> resultCallback) {
        super.invokeAsyncImpl(getChannel(login), request, timeoutMillis, resultCallback);
    }

    @Override
    public void invokeOneway(String login, RemotingCommand request, long timeoutMillis, ResultCallback<Void> resultCallback) {
        super.invokeOnewayImpl(getChannel(login), request, timeoutMillis, resultCallback);
    }

    @Override
    public boolean isConnected(String login) {
        return this.channelTable.containsKey(login);
    }

    private Channel getChannel(String login) {
        return this.channelTable.get(login);
    }

    @ChannelHandler.Sharable
    class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommandProtobuf> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommandProtobuf msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }

    @ChannelHandler.Sharable
    class NettyServerConnectManageHandler extends NettyAbstractConnectManageHandler {
        public NettyServerConnectManageHandler() {
            super("SERVER", NettyRemotingServer.this.channelEventListener);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            super.userEventTriggered(ctx, evt);

            if (evt instanceof AuthenticationEvent) {
                AuthenticationEvent event = (AuthenticationEvent) evt;
                if (AuthenticationEvent.SUCCESS == event) {
                    String currentLogin = RemotingSecurityUtils.getCurrentLogin(ctx.channel());
                    NettyRemotingServer.this.channelTable.put(currentLogin, ctx.channel());
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            NettyRemotingServer.this.channelTable.remove(RemotingSecurityUtils.getCurrentLogin(ctx.channel()));
        }
    }
}
