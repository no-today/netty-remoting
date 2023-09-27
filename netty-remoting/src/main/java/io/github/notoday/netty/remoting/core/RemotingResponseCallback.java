package io.github.notoday.netty.remoting.core;

import io.github.notoday.netty.remoting.protocol.RemotingCommand;

/**
 * @author no-today
 * @date 2022/07/01 11:36
 */
public interface RemotingResponseCallback {

    void callback(RemotingCommand response);
}
