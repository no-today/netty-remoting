package io.github.notoday.netty.remoting.protocol;

import com.google.protobuf.Any;
import io.github.notoday.netty.remoting.protocol.protobuf.AuthenticationToken;
import io.github.notoday.netty.remoting.protocol.protobuf.RemotingCommandProtobuf;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author no-today
 * @date 2022/07/10 01:16
 */
public class RemotingCommandTest {

    @Test
    public void protobuf() {
        RemotingCommandProtobuf protobuf = RemotingCommandProtobuf.newBuilder()
                .setVersion(1)
                .setFlag(1)
                .setCode(1)
                .setRemark("ok")
                .setReqId(1)
                .setBody(Any.pack(AuthenticationToken.newBuilder().setToken(random()).setLogin("no-today").build()))
                .putExtFields("random", "UUID")
                .putImplicitFields("traceId", UUID.randomUUID().toString())
                .build();

        System.out.println(protobuf);
        System.out.println(RemotingCommand.of(protobuf));
        System.out.println(RemotingCommand.of(protobuf));
    }

    @Test
    public void mapperOfBytes() {
        byte[] body = "no-today".getBytes();

        RemotingCommand command = new RemotingCommand().setBody(body);
        RemotingCommandProtobuf remoting = command.protobuf();

        RemotingCommand cmd = RemotingCommand.of(remoting);
        RemotingCommandProtobuf rmt = cmd.protobuf();

        assertArrayEquals(command.getBody(), cmd.getBody());
        assertArrayEquals(remoting.getBody().toByteArray(), rmt.getBody().toByteArray());
    }

    @Test
    public void mapperOfProtobuf() {
        RemotingCommandProtobuf remoting = RemotingCommandProtobuf.newBuilder()
                .setBody(Any.pack(AuthenticationToken.newBuilder().setToken(random()).setLogin("no-today").build()))
                .build();
        RemotingCommand command = RemotingCommand.of(remoting);

        RemotingCommandProtobuf rmt = command.protobuf();
        RemotingCommand cmd = RemotingCommand.of(rmt);

        assertArrayEquals(command.getBody(), cmd.getBody());
        assertArrayEquals(remoting.getBody().toByteArray(), rmt.getBody().toByteArray());
    }

    private String random() {
        return UUID.randomUUID().toString();
    }
}