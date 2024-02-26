package io.github.notoday.netty.remoting;

import io.github.notoday.netty.remoting.core.RemotingResponseCallback;
import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author no-today
 * @date 2022/05/29 16:58
 */
public interface NettyRequestProcessor {

    /**
     * 拒绝请求
     */
    default boolean rejectRequest() {
        return false;
    }

    /**
     * 处理请求
     *
     * @param ctx     channel handler context
     * @param request request
     * @return response
     */
    RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception;

    default void asyncProcessRequest(ChannelHandlerContext ctx, RemotingCommand request, RemotingResponseCallback callback) throws Exception {
        RemotingCommand response = processRequest(ctx, request);
        callback.callback(response);
    }
}
