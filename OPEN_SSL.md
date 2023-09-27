```shell
# 生成 2048 位RSA私钥  
openssl genrsa -out server.key 2048  

# 根据 server.key 私钥生成证书签名请求(CSR)  
openssl req -new -key server.key -out server.csr  

# 自签名证书,使用 server.key 对 server.csr 文件进行签名,生成 server.crt 证书,有效期365天  
openssl x509 -req \
            -days 365 \
            -in server.csr \ 
            -signkey server.key \
            -out server.crt

# 将 server.key 私钥转成加密 PKCS#8 格式,输出到 server.key.encrypted 文件中,但没有设置加密密码(-nocrypt)  
openssl pkcs8 -topk8 \
           -inform PEM \
           -outform PEM \  
           -in server.key \
           -out server.key.encrypted \
           -nocrypt
```
```