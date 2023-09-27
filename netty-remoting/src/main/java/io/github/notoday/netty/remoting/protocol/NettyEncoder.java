package io.github.notoday.netty.remoting.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author no-today
 * @date 2023/09/21 17:17
 */
@ChannelHandler.Sharable
public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RemotingCommand command, ByteBuf out) throws Exception {
        command.encode(out);
    }
}
