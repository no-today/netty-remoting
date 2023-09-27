package org.apache.skywalking.apm.plugin;

import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;
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

import static org.apache.skywalking.apm.plugin.NettyRemotingHelper.TAG_REQUEST;
import static org.apache.skywalking.apm.plugin.NettyRemotingHelper.generateOperationName;

/**
 * Start 客户端发起请求
 *
 * @author no-today
 * @date 2022/10/26 11:10
 */
public class NettyRemotingClientInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance enhancedInstance, Method method, Object[] args, Class<?>[] classes, MethodInterceptResult methodInterceptResult) throws Throwable {
        final Channel channel = (Channel) args[0];
        final RemotingCommand command = (RemotingCommand) args[1];

        final ContextCarrier contextCarrier = new ContextCarrier();
        final AbstractSpan span = ContextManager.createExitSpan(generateOperationName(command), contextCarrier, getRemotePeer(channel));
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            command.putExtFields(next.getHeadKey(), next.getHeadValue());
        }

        span.tag(TAG_REQUEST, command.toString());

        span.setComponent(ComponentsDefine.TRANSPORT_CLIENT);
        SpanLayer.asRPCFramework(span);
    }

    private String getRemotePeer(Channel channel) {
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        return remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();
    }

    @Override
    public Object afterMethod(EnhancedInstance enhancedInstance, Method method, Object[] args, Class<?>[] classes, Object result) throws Throwable {
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
