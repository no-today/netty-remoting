package io.github.notoday.netty.remoting.security;

import io.github.notoday.netty.remoting.common.RemotingSystemCode;
import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.github.notoday.netty.remoting.common.RemotingUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@ChannelHandler.Sharable
public class NettyAuthenticatorHandler extends SimpleChannelInboundHandler<RemotingCommand> {

    private static final AttributeKey<Integer> REQUEST_ID = AttributeKey.valueOf("_RequestId");

    private final Authenticator authenticator;

    public NettyAuthenticatorHandler(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    private static void bindAuthentication(Channel channel, Authentication authentication) {
        channel.attr(RemotingSecurityUtils.AUTHENTICATION).set(authentication);
    }

    private static void bindReqId(final Channel channel, int requestId) {
        channel.attr(REQUEST_ID).set(requestId);
    }

    private static void unbindReqId(final Channel channel) {
        channel.attr(REQUEST_ID).set(null);
    }

    private static int getReqId(final Channel channel) {
        return Optional.ofNullable(channel.attr(REQUEST_ID)).map(Attribute::get).orElse(-1);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
        bindReqId(ctx.channel(), msg.getReqId());

        AuthenticationToken token = msg.unpack(AuthenticationToken.class);
        Authentication authentication;
        if (this.authenticator == null) {
            // default
            authentication = new Authentication(token.getLogin(), "", List.of("ANONYMOUS_USER"));
        } else {
            authentication = this.authenticator.authenticate(ctx.channel(), token);
        }

        unbindReqId(ctx.channel());
        bindAuthentication(ctx.channel(), authentication);
        ctx.pipeline().remove(this);

        log.debug("NETTY AUTHENTICATION SUCCESS: {}", authentication.getPrincipal());
        ctx.writeAndFlush(RemotingCommand.success(msg.getReqId()));

        // Publish event
        ctx.fireUserEventTriggered(AuthenticationEvent.SUCCESS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        int reqId = getReqId(ctx.channel());
        log.warn("NETTY AUTHENTICATION EXCEPTION: {}", reqId, cause);
        ctx.writeAndFlush(RemotingCommand.failure(reqId, RemotingSystemCode.UNAUTHORIZED, cause.getMessage()));
        RemotingUtil.closeChannel(ctx.channel());
    }
}