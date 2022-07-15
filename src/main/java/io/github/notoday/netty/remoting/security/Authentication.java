package io.github.notoday.netty.remoting.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author no-today
 * @date 2022/06/23 17:00
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Authentication {

    private String principal;
    private String credentials;
    private List<String> authorities;
}
