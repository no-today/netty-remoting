package io.github.notoday.netty.remoting.ssl;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import javax.net.ssl.SSLException;
import java.io.InputStream;

/**
 * @author no-today
 * @date 2023/09/22 16:21
 */
public class OpenSslProvider {

    public static SslContext server() {
        try {
            return SslContextBuilder.forServer(getResource("server.crt"), getResource("server.key.encrypted"))
                    .sslProvider(SslProvider.OPENSSL)
                    .clientAuth(ClientAuth.NONE)
//                    .protocols("TLSv1.2")
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    public static SslContext client() {
        try {
            return SslContextBuilder.forClient()
                    .sslProvider(SslProvider.OPENSSL)
//                    .trustManager(InsecureTrustManagerFactory.INSTANCE) // 信任所有证书
                    .trustManager(getResource("server.crt"))        // 只信任自签名证书
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream getResource(String name) {
        return OpenSslProvider.class.getClassLoader().getResourceAsStream(name);
    }
}
