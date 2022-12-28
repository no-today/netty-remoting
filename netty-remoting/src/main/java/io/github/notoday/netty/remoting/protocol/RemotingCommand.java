package io.github.notoday.netty.remoting.protocol;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.InvalidProtocolBufferException;
import io.github.notoday.netty.remoting.common.RemotingSysResponseCode;
import io.github.notoday.netty.remoting.protocol.protobuf.RemotingCommandProtobuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author no-today
 * @date 2022/07/06 11:37
 */
@Data
@Accessors(chain = true)
public class RemotingCommand implements Serializable {
    private static final long serialVersionUID = 2637522862988253773L;

    private static final AtomicInteger requestId = new AtomicInteger();

    private static final byte RESPONSE = 1;
    private static final byte ONEWAY = 2;

    private int version;
    private int reqId;
    private int flag;
    private int code;
    private String remark;
    private byte[] body;
    private Map<String, String> extFields;
    private Map<String, String> implicitFields;

    /**
     * 标记位
     *
     * @param bit > 0 && < 32
     * @return after mark flag
     */
    private static int mark(int flag, int bit) {
        return flag | (1 << (bit - 1));
    }

    // ------------------------------------------------------------
    public static RemotingCommand request(int version, int code, byte[] body, Map<String, String> extFields) {
        return new RemotingCommand().setReqId(requestId.getAndIncrement()).setVersion(version).setCode(code).setBody(body).setExtFields(extFields);
    }

    public static RemotingCommand request(int code, byte[] body, Map<String, String> extFields) {
        return request(0, code, body, extFields);
    }

    public static RemotingCommand request(int version, int code, byte[] body) {
        return request(version, code, body, null);
    }

    public static RemotingCommand request(int code, byte[] body) {
        return request(0, code, body, null);
    }

    // ------------------------------------------------------------

    public static RemotingCommand success(int version, int reqId, byte[] body, Map<String, String> extFields) {
        return new RemotingCommand().markResponseType().setCode(RemotingSysResponseCode.SUCCESS).setVersion(version).setReqId(reqId).setBody(body).setExtFields(extFields);
    }

    public static RemotingCommand success(int reqId, byte[] body, Map<String, String> extFields) {
        return success(0, reqId, body, extFields);
    }

    public static RemotingCommand success(int version, int reqId) {
        return success(version, reqId, null, null);
    }

    public static RemotingCommand success(int reqId) {
        return success(0, reqId, null, null);
    }

    public static RemotingCommand failure(int version, int reqId, int code, String remark) {
        return new RemotingCommand().markResponseType().setVersion(version).setReqId(reqId).setCode(code).setRemark(remark);
    }

    public static RemotingCommand failure(int reqId, int code, String remark) {
        return failure(0, reqId, code, remark);
    }

    // ------------------------------------------------------------

    public static RemotingCommand of(RemotingCommandProtobuf command) {
        RemotingCommand cmd = new RemotingCommand().setVersion(command.getVersion()).setReqId(command.getReqId()).setFlag(command.getFlag()).setCode(command.getCode()).setRemark(command.getRemark()).setExtFields(command.getExtFieldsMap()).setImplicitFields(command.getImplicitFieldsMap());

        if (command.hasBody()) {
            // 如果是 bytes, 则还原成字节数组, 否则直接用 protobuf 编码
            if (command.getBody().getTypeUrl().endsWith(BytesValue.getDescriptor().getFullName())) {
                try {
                    cmd.setBody(command.getBody().unpack(BytesValue.class).getValue().toByteArray());
                } catch (InvalidProtocolBufferException e) {
                    // 实际上不可能走到这里
                    throw new RuntimeException(e);
                }
            } else {
                cmd.setBody(command.getBody().toByteArray());
            }
        }

        return cmd;
    }

    public String getExtFieldsOrDefault(String key, String defaultValue) {
        return this.extFields == null ? defaultValue : this.extFields.getOrDefault(key, defaultValue);
    }

    public RemotingCommand putExtFields(String key, String value) {
        if (this.extFields == null) {
            this.extFields = new HashMap<>();
        }

        try {
            this.extFields.put(key, value);
        } catch (UnsupportedOperationException e) {
            // Collections$UnmodifiableMap
            this.extFields = new HashMap<>(this.extFields);
            this.extFields.put(key, value);
        }
        return this;
    }

    public String getImplicitFields(String key) {
        return this.implicitFields == null ? null : this.implicitFields.get(key);
    }

    public RemotingCommand putImplicitFields(String key, String value) {
        if (this.implicitFields == null) {
            this.implicitFields = new HashMap<>();
        }

        try {
            this.implicitFields.put(key, value);
        } catch (UnsupportedOperationException e) {
            // Collections$UnmodifiableMap
            this.extFields = new HashMap<>(this.extFields);
            this.extFields.put(key, value);
        }

        return this;
    }

    public boolean success() {
        if (!isResponse()) return false;
        return code == RemotingSysResponseCode.SUCCESS;
    }

    public RemotingCommand markResponseType() {
        this.flag = mark(this.flag, RESPONSE);
        return this;
    }

    public void markOnewayRPC() {
        this.flag = mark(this.flag, ONEWAY);
    }

    public boolean isResponse() {
        return (this.flag & RESPONSE) == RESPONSE;
    }

    // ------------------------------------------------------------

    public boolean isOneway() {
        return (this.flag & ONEWAY) == ONEWAY;
    }

    public RemotingCommandProtobuf protobuf() {
        RemotingCommandProtobuf.Builder builder = RemotingCommandProtobuf.newBuilder().setReqId(getReqId()).setFlag(getFlag()).setCode(getCode());

        if (getRemark() != null) builder.setRemark(getRemark());
        if (getExtFields() != null) builder.putAllExtFields(getExtFields());
        if (getImplicitFields() != null) builder.putAllImplicitFields(getImplicitFields());

        if (getBody() != null) {
            try {
                builder.setBody(Any.parseFrom(getBody()));
            } catch (InvalidProtocolBufferException e) {
                // 不是 protobuf 编码, 使用 Bytes
                builder.setBody(Any.pack(BytesValue.of(ByteString.copyFrom(getBody()))));
            }
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return "{\n" +
                "  \"version\": " + version + ",\n" +
                "  \"reqId\": " + reqId + ",\n" +
                "  \"flag\": " + flag + ",\n" +
                "  \"code\": " + code + ",\n" +
                "  \"remark\": \"" + remark + "\",\n" +
                "  \"bodyLength\": " + (body == null ? 0 : body.length) + ",\n" +
                "  \"extFields\": {\n" +
                extToString() +
                "  }\n" +
                "}";
    }

    private String extToString() {
        if (extFields == null || extFields.isEmpty()) {
            return "";
        }

        return extFields.entrySet().stream()
                .map(e -> "    \"" + e.getKey() + "\": \"" + e.getValue() + "\"")
                .collect(Collectors.joining(",\n")) + "\n";
    }
}
