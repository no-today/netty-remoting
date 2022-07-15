package io.github.notoday.netty.remoting;

import io.github.notoday.netty.remoting.protocol.RemotingCommand;

/**
 * 服务端: 消息处理前后调用
 * 客户端: 消息发送前调用, 需要注意的是只有同步调用会触发后置勾子, 异步调用需要到回调里处理请求闭环
 *
 * @author no-today
 * @date 2022/06/29 11:49
 */
public interface RPCHook {

    void doBeforeRequest(final String login, final RemotingCommand request);

    void doAfterResponse(final String login, final RemotingCommand request, final RemotingCommand response);
}
