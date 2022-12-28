package org.apache.skywalking.apm.plugin;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

/**
 * @author no-today
 * @date 2022/10/26 11:02
 */
public class NettyRemotingClientInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName("io.github.notoday.netty.remoting.core.NettyRemotingAbstract");
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{new InstanceMethodsInterceptPoint() {
            @Override
            public ElementMatcher<MethodDescription> getMethodsMatcher() {
                return ElementMatchers.named("invokeSyncImpl")
                        .or(ElementMatchers.named("invokeAsyncImpl"))
                        .or(ElementMatchers.named("invokeOnewayImpl"));
            }

            @Override
            public String getMethodsInterceptor() {
                return "org.apache.skywalking.apm.plugin.NettyRemotingClientInterceptor";
            }

            @Override
            public boolean isOverrideArgs() {
                return false;
            }
        }};
    }
}
