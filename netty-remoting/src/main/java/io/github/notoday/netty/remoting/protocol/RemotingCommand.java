package io.github.notoday.netty.remoting.protocol;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import io.github.notoday.netty.remoting.common.RemotingSystemCode;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author no-today
 * @date 2022/07/06 11:37
 */
@Data
@Accessors(chain = true)
public class RemotingCommand implements Serializable {
    private static final long serialVersionUID = 2637522862988253773L;

    static final int MAGIC_NUMBER = 0x951AEFC8;
    private static final AtomicInteger requestId = new AtomicInteger();

    private final static byte REQUEST = 0x00;
    private final static byte RESPONSE = 0x01;
    private final static byte ONEWAY = 0x02;

    /**
     * 协议版本号
     */
    private byte version;

    /**
     * 请求ID, 用于串联应答
     */
    private int reqId;

    /**
     * 指令类型
     */
    private int type;

    /**
     * 指令编码 or 响应编码
     */
    private int code;

    /**
     * 响应时的文本消息, 通常用于告知异常信息
     */
    private String message;

    /**
     * 二进制内容
     * <p>
     * Any
     */
    private byte[] body;

    /**
     * 扩展字段, 用于透传额外的信息, 例如 traceId
     */
    private Map<String, String> extFields;

    public RemotingCommand markResponseType() {
        this.type = RESPONSE;
        return this;
    }

    public RemotingCommand markOnewayRPC() {
        this.type = ONEWAY;
        return this;
    }

    @JSONField(serialize = false)
    public boolean isResponse() {
        return RESPONSE == type;
    }

    @JSONField(serialize = false)
    public boolean isOneway() {
        return ONEWAY == type;
    }

    @JSONField(serialize = false)
    public boolean success() {
        if (!isResponse()) return false;
        return RemotingSystemCode.SUCCESS == code;
    }


    public String getExtFieldsOrDefault(String key, String defaultValue) {
        return Optional.ofNullable(this.extFields)
                .map(e -> e.getOrDefault(key, defaultValue))
                .orElse(null);
    }

    public RemotingCommand putExtFields(String key, String value) {
        if (this.extFields == null) this.extFields = new HashMap<>();
        this.extFields.put(key, value);
        return this;
    }

    // ------------------------------------------------------------

    public void encode(ByteBuf out) {
        out.writeInt(MAGIC_NUMBER);

        byte[] bytes = JSON.toJSONString(this).getBytes(Charset.defaultCharset());

        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

    public <T> T unpack(Class<T> clazz) {
        return JSON.parseObject(new String(body), clazz);
    }

    // ------------------------------------------------------------

    public static RemotingCommand request(byte version, int code, byte[] body, Map<String, String> extFields) {
        return new RemotingCommand().setReqId(requestId.getAndIncrement()).setVersion(version).setCode(code).setBody(body).setExtFields(extFields);
    }

    public static RemotingCommand request(int code, byte[] body, Map<String, String> extFields) {
        return request((byte) 0, code, body, extFields);
    }

    public static RemotingCommand request(byte version, int code, byte[] body) {
        return request(version, code, body, null);
    }

    public static RemotingCommand request(int code, byte[] body) {
        return request((byte) 0, code, body, null);
    }

    // ------------------------------------------------------------

    public static RemotingCommand success(byte version, int reqId, byte[] body, Map<String, String> extFields) {
        return new RemotingCommand().markResponseType().setCode(RemotingSystemCode.SUCCESS).setVersion(version).setReqId(reqId).setBody(body).setExtFields(extFields);
    }

    public static RemotingCommand success(int reqId, byte[] body, Map<String, String> extFields) {
        return success((byte) 0, reqId, body, extFields);
    }

    public static RemotingCommand success(byte version, int reqId) {
        return success(version, reqId, null, null);
    }

    public static RemotingCommand success(int reqId) {
        return success((byte) 0, reqId, null, null);
    }

    public static RemotingCommand failure(byte version, int reqId, int code, String remark) {
        return new RemotingCommand().markResponseType().setVersion(version).setReqId(reqId).setCode(code).setMessage(remark);
    }

    public static RemotingCommand failure(int reqId, int code, String remark) {
        return failure((byte) 0, reqId, code, remark);
    }
}
