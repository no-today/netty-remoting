package io.github.notoday.netty.remoting.protocol;

import com.alibaba.fastjson2.JSON;
import io.github.notoday.netty.remoting.config.NettySystemConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import static io.github.notoday.netty.remoting.protocol.RemotingCommand.MAGIC_NUMBER;

/**
 * 粘包
 * - 概念: 将多个数据包粘贴在一起,作为一个整包发送出去。
 * - 原因: 网络库发送数据时,多个发送操作合并成一次操作。或者数据包正好在网络节点缓冲区最后,和下一包数据粘在一起。
 * - 解决: 添加消息边界标识,如包长度字段;或者使用消息定长方式。
 * <p>
 * 半包
 * - 概念:一个数据包被分成多个包发送,接收端收到时是不完整数据。
 * - 原因:网络环境原因,或一次发送的数据过大被拆分。
 * - 解决:添加包体长度信息,接收端可以重新组装整包;或者使用缓存机制,直到收全包再处理。
 * <p>
 * 在知道整包长度的情况下使用 LengthFieldBasedFrameDecoder 会很方便。
 * 它会等到一个完整的包之后再返回
 *
 * @author no-today
 * @date 2023/09/21 16:42
 */
@Slf4j
public class NettyDecoder extends LengthFieldBasedFrameDecoder {

    public NettyDecoder() {
        /*
         * |    4 byte   | 4 byte |  N byte |
         * | MagicNumber | Length | Content |
         */
        super(NettySystemConfig.frameMaxLength, 4, 4, 0, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        try {
            // 只有是魔术开头的包才会处理
            in.markReaderIndex();
            if (in.readableBytes() < 4 || MAGIC_NUMBER != in.readInt()) return null;
        } finally {
            in.resetReaderIndex();
        }

        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        // skip magic number
        frame.skipBytes(4);

        byte[] bytes = new byte[frame.readInt()];
        frame.readBytes(bytes);

        return JSON.parseObject(bytes, RemotingCommand.class);
    }
}
