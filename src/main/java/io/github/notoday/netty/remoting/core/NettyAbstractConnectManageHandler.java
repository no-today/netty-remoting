package io.github.notoday.netty.remoting.core;

import io.github.notoday.netty.remoting.ChannelEventListener;
import io.github.notoday.netty.remoting.common.RemotingUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @author no-today
 * @date 2022/06/17 10:40
 */
@Slf4j
public abstract class NettyAbstractConnectManageHandler extends ChannelDuplexHandler {

    private final String tag;
    private final ChannelEventListener eventListener;

    public NettyAbstractConnectManageHandler(String tag, ChannelEventListener eventListener) {
        this.tag = tag;
        this.eventListener = eventListener;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY {} PIPELINE: [{}] channelRegistered", tag, remoteAddress);
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY {} PIPELINE: [{}] channelUnregistered", tag, remoteAddress);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY {} PIPELINE: [{}] channelActive", tag, remoteAddress);
        super.channelActive(ctx);

        if (this.eventListener != null) {
            this.eventListener.onChannelConnect(remoteAddress, ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        log.debug("NETTY {} PIPELINE: [{}] channelInactive", tag, remoteAddress);
        super.channelInactive(ctx);

        if (this.eventListener != null) {
            this.eventListener.onChannelClose(remoteAddress, ctx.channel());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
                log.debug("NETTY {} PIPELINE: [{}] idleEvent", tag, remoteAddress);
                RemotingUtil.closeChannel(ctx.channel());

                if (this.eventListener != null) {
                    this.eventListener.onChannelIdle(remoteAddress, ctx.channel());
                }
            }
        }

        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String remoteAddress = RemotingUtil.parseChannelRemoteAddr(ctx.channel());
        log.warn("NETTY {} PIPELINE: [{}] exceptionCaught ", tag, remoteAddress, cause);
        RemotingUtil.closeChannel(ctx.channel());

        if (this.eventListener != null) {
            this.eventListener.onChannelException(remoteAddress, ctx.channel());
        }
    }
}
