package io.github.notoday.netty.remoting.security;

import io.github.notoday.netty.remoting.common.RemotingSysResponseCode;
import io.github.notoday.netty.remoting.common.RemotingUtil;
import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.github.notoday.netty.remoting.protocol.protobuf.AuthenticationToken;
import io.github.notoday.netty.remoting.protocol.protobuf.RemotingCommandProtobuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ChannelHandler.Sharable
public class NettyAuthenticatorHandler extends SimpleChannelInboundHandler<RemotingCommandProtobuf> {

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
        return channel.attr(REQUEST_ID).get();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommandProtobuf msg) throws Exception {
        bindReqId(ctx.channel(), msg.getReqId());

        AuthenticationToken token = msg.getBody().unpack(AuthenticationToken.class);
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
        ctx.writeAndFlush(RemotingCommand.success(msg.getReqId()).protobuf());

        // Publish event
        ctx.fireUserEventTriggered(AuthenticationEvent.SUCCESS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        int reqId = getReqId(ctx.channel());
        log.warn("NETTY AUTHENTICATION EXCEPTION: {}", reqId, cause);
        ctx.writeAndFlush(RemotingCommand.failure(reqId, RemotingSysResponseCode.UNAUTHORIZED, cause.getMessage()).protobuf());
        RemotingUtil.closeChannel(ctx.channel());
    }
}