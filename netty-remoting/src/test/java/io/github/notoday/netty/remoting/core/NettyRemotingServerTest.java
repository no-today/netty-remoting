package io.github.notoday.netty.remoting.core;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;
import io.github.notoday.netty.remoting.NettyRequestProcessor;
import io.github.notoday.netty.remoting.RemotingProcessable;
import io.github.notoday.netty.remoting.common.RemotingSysResponseCode;
import io.github.notoday.netty.remoting.config.NettyClientConfig;
import io.github.notoday.netty.remoting.config.NettyServerConfig;
import io.github.notoday.netty.remoting.exception.RemotingConnectException;
import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author no-today
 * @date 2022/06/23 18:15
 */
public class NettyRemotingServerTest {

    ResultCallback<RemotingCommand> callback = new ResultCallback<>() {
        @Override
        public void onSuccess(RemotingCommand response) {
        }

        @Override
        public void onFailure(ErrorInfo error) {
            System.err.println(error);
        }
    };

    @Test
    public void listening() throws Exception {
        NettyRemotingServer server = new NettyRemotingServer(new NettyServerConfig(), null, null);
        registerRequestProcessor(server, new AtomicBoolean(false), System.out::println);

        server.start();

        TimeUnit.SECONDS.sleep(5);
        server.shutdown();
    }

    @Test
    public void login() throws Exception {
        NettyRemotingServer server = new NettyRemotingServer(new NettyServerConfig(), null, null);
        registerRequestProcessor(server, new AtomicBoolean(false), null);
        server.start();

        NettyRemotingClient client = new NettyRemotingClient(new NettyClientConfig(), null);

        // not login throws exception
        assertThrows(RemotingConnectException.class, () -> client.invokeSync(RemotingCommand.request(0, null, null), 200));

        client.login("", randomString(), callback);

        // login success after, ok
        assertEquals(RemotingSysResponseCode.SUCCESS, client.invokeSync(RemotingCommand.request(0, null, null), 200).getCode());

        server.shutdown();
        client.shutdown();
    }

    @Test
    public void sysErrors() throws Exception {
        NettyRemotingServer server = new NettyRemotingServer(new NettyServerConfig(), null, null);
        server.start();

        NettyRemotingClient client = new NettyRemotingClient(new NettyClientConfig(), null);
        client.login("", randomString(), callback);

        assertEquals(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED, client.invokeSync(RemotingCommand.request(1024, Any.pack(StringValue.of(randomString())).toByteArray(), Map.of("random", randomString())), 200).getCode());

        // 原封不动返回请求数据
        AtomicBoolean rejectRequest = new AtomicBoolean(true);
        registerRequestProcessor(server, rejectRequest, null);

        assertEquals(RemotingSysResponseCode.COMMAND_NOT_AVAILABLE_NOW, client.invokeSync(RemotingCommand.request(1024, Any.pack(StringValue.of(randomString())).toByteArray(), Map.of("random", randomString())), 200).getCode());
        rejectRequest.set(false);

        int count = 100000;
        CountDownLatch cd = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            client.invokeAsync(RemotingCommand.request(1024, Any.pack(StringValue.of(randomString())).toByteArray(), Map.of("random", randomString())), 3000, callback(cd));
        }

        cd.await();

        client.shutdown();
        server.shutdown();
    }

    private ResultCallback<RemotingCommand> callback(CountDownLatch cd) {
        return new ResultCallback<>() {
            @Override
            public void onSuccess(RemotingCommand response) {
                cd.countDown();
            }

            @Override
            public void onFailure(ErrorInfo error) {
                cd.countDown();
                System.err.println(error);
            }
        };
    }

    private ResultCallback<Void> callbackVoid(CountDownLatch cd) {
        return new ResultCallback<>() {
            @Override
            public void onSuccess(Void response) {
                cd.countDown();
            }

            @Override
            public void onFailure(ErrorInfo error) {
                cd.countDown();
                System.err.println(error);
            }
        };
    }

    private void registerRequestProcessor(RemotingProcessable server, AtomicBoolean rejectRequest, Consumer<RemotingCommand> consumer) {
        server.registerDefaultProcessor(new NettyRequestProcessor() {
            @Override
            public boolean rejectRequest() {
                return rejectRequest.get();
            }

            @Override
            public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
                if (consumer != null) consumer.accept(request);
                return request.setCode(0);
            }
        }, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    private String randomString() {
        return randomString(1);
    }

    private String randomString(int count) {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < count; i++) {
            r.append(UUID.randomUUID());
        }
        return r.toString();
    }

    @Test
    public void callClient() throws Exception {
        String login = "no-today";

        NettyRemotingServer server = new NettyRemotingServer(new NettyServerConfig(), null, null);
        NettyRemotingClient client = new NettyRemotingClient(new NettyClientConfig(), null);

        AtomicInteger requestCounter = new AtomicInteger(0);
        AtomicInteger onewayCounter = new AtomicInteger(0);

        registerRequestProcessor(client, new AtomicBoolean(false), req -> {
            requestCounter.incrementAndGet();
            if (req.isOneway()) onewayCounter.incrementAndGet();
        });

        server.start();
        client.login("", login, callback);

        int count = 10000;
        for (int i = 0; i < count; i++) {
            System.out.println(server.invokeSync(login, RemotingCommand.request(1024, Any.pack(StringValue.of(randomString(10))).toByteArray()), 200));
        }

        CountDownLatch cd = new CountDownLatch(count * 2);
        for (int i = 0; i < count; i++) {
            server.invokeOneway(login, RemotingCommand.request(1024, Any.pack(StringValue.of(randomString(10))).toByteArray()), 200, callbackVoid(cd));
        }

        for (int i = 0; i < count; i++) {
            server.invokeAsync(login, RemotingCommand.request(1024, Any.pack(StringValue.of(randomString(10))).toByteArray()), 200, callback(cd));
        }

        cd.await();

        assertEquals(count, onewayCounter.get());
        assertEquals(count * 3, requestCounter.get());

        client.shutdown();
        server.shutdown();
    }
}