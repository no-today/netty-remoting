package io.github.notoday.netty.remoting;

import io.netty.channel.Channel;

/**
 * @author no-today
 * @date 2022/06/17 11:14
 */
public interface ChannelEventListener {

    void onChannelConnect(final String login, final Channel channel);

    void onChannelClose(final String login, final Channel channel);

    void onChannelException(final String login, final Channel channel);

    void onChannelIdle(final String login, final Channel channel);
}
