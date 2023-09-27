package io.github.notoday.netty.remoting.protocol;

import com.alibaba.fastjson2.JSON;

/**
 * @author no-today
 * @date 2023/09/22 11:02
 */
public class Any {

    public static byte[] pack(Object obj) {
        return JSON.toJSONBytes(obj);
    }

    public static <T> T unpack(byte[] bytes, Class<T> clazz) {
        return JSON.parseObject(bytes, clazz);
    }
}
