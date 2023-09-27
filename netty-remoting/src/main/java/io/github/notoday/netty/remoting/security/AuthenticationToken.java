package io.github.notoday.netty.remoting.security;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author no-today
 * @date 2023/09/22 09:56
 */
@Data
@Accessors(chain = true)
public class AuthenticationToken {

    private String token;
    private String login;
}
