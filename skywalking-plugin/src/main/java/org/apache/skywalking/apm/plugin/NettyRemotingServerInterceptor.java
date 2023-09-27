package org.apache.skywalking.apm.plugin;

import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.github.notoday.netty.remoting.security.RemotingSecurityUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;

import static org.apache.skywalking.apm.plugin.NettyRemotingHelper.*;

/**
 * Start 客户端发起请求
 *
 * @author no-today
 * @date 2022/10/26 11:10
 */
public class NettyRemotingServerInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance enhancedInstance, Method method, Object[] args, Class<?>[] classes, MethodInterceptResult methodInterceptResult) throws Throwable {
        final Channel channel = ((ChannelHandlerContext) args[0]).channel();
        final RemotingCommand command = (RemotingCommand) args[1];

        final ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(command.getExtFieldsOrDefault(next.getHeadKey(), null));
        }

        final AbstractSpan span = ContextManager.createEntrySpan(generateOperationName(command), contextCarrier);
        span.setPeer(getRemotePeer(channel));

        span.tag(TAG_LOGIN, RemotingSecurityUtils.getCurrentLogin(channel));
        span.tag(TAG_PROCESSOR, enhancedInstance.getClass().getSimpleName());
        span.setComponent(ComponentsDefine.TRANSPORT_CLIENT);
        SpanLayer.asRPCFramework(span);
    }

    private String getRemotePeer(Channel channel) {
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        return remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();
    }

    @Override
    public Object afterMethod(EnhancedInstance enhancedInstance, Method method, Object[] args, Class<?>[] classes, Object result) throws Throwable {
        AbstractSpan span = ContextManager.activeSpan();
        span.tag(TAG_RESPONSE, Objects.toString(result));
        span.tag(TAG_RESPONSE_CODE, String.valueOf(Optional.ofNullable(result).map(e -> ((RemotingCommand) e).getCode()).orElse(null)));
        ContextManager.stopSpan();
        return result;
    }

    @Override
    public void handleMethodException(EnhancedInstance enhancedInstance, Method method, Object[] args, Class<?>[] classes, Throwable throwable) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().log(throwable);
        }
    }
}
