package io.github.notoday.netty.remoting.ssl;

import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * @author no-today
 * @date 2023/09/25 17:01
 */
public class X509CertificateTest {

    public static InputStream getResource(String name) {
        return X509CertificateTest.class.getClassLoader().getResourceAsStream(name);
    }

    @Test
    public void parseTest() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) factory.generateCertificate(getResource("server.crt"));

        print("版本号", certificate.getVersion());
        print("主题信息", certificate.getSubjectX500Principal());
        print("签发者信息", certificate.getIssuerX500Principal());

        RSAPublicKey rsaPublicKey = (RSAPublicKey) certificate.getPublicKey();
        print("序列号", String.format("%064X", certificate.getSerialNumber()).toLowerCase());
        print("加密算法", rsaPublicKey.getAlgorithm());
        print("密钥强度", rsaPublicKey.getModulus().bitLength());
        print("签名算法", certificate.getSigAlgName());
        print("颁发时间", formatter.format(certificate.getNotBefore()));
        print("过期时间", formatter.format(certificate.getNotAfter()));
        print("剩余天数", ChronoUnit.DAYS.between(Instant.now(), certificate.getNotAfter().toInstant()));
        print("SHA1指纹", Hex.encodeHexString(MessageDigest.getInstance("SHA-1").digest(certificate.getEncoded())));
        print("SHA2指纹", Hex.encodeHexString(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded())));
        print("签名", Hex.encodeHexString(certificate.getSignature()).replaceAll("(.{64})", "$1\n"));

        String publicKey =
                "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded()).replaceAll("(.{64})", "$1\n") + "\n" +
                "-----END PUBLIC KEY-----";
        print("公钥", publicKey);
        print("PN(e) 公钥指数e", rsaPublicKey.getPublicExponent());
        print("PN(n) 公钥指数n", rsaPublicKey.getModulus().toString().replaceAll("(.{64})", "$1\n"));
    }

    private void print(String name, Object obj) {
        System.out.println("-----[" + name + "]");
        System.out.println(obj);
    }
}
