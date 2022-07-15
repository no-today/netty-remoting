package io.github.notoday.netty.remoting.security;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.Optional;

/**
 * @author no-today
 * @date 2022/06/23 22:22
 */
public class RemotingSecurityUtils {

    static final AttributeKey<Authentication> AUTHENTICATION = AttributeKey.valueOf("Authentication");

    public static Optional<Authentication> getAuthentication(final Channel channel) {
        return Optional.ofNullable(channel.attr(AUTHENTICATION).get());
    }

    public static String getCurrentLogin(final Channel channel) {
        return Optional.ofNullable(channel.attr(AUTHENTICATION).get()).map(Authentication::getPrincipal).orElse("");
    }
}
