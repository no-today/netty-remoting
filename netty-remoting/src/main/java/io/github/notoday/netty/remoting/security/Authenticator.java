package io.github.notoday.netty.remoting.security;

import io.github.notoday.netty.remoting.exception.AuthenticationException;
import io.github.notoday.netty.remoting.protocol.protobuf.AuthenticationToken;
import io.netty.channel.Channel;

/**
 * @author no-today
 * @date 2022/06/23 16:52
 */
public interface Authenticator {

    /**
     * 认证
     *
     * @param channel             通道
     * @param authenticationToken 认证身份信息
     * @throws AuthenticationException 认证失败抛出异常
     */
    Authentication authenticate(Channel channel, AuthenticationToken authenticationToken) throws AuthenticationException;
}
