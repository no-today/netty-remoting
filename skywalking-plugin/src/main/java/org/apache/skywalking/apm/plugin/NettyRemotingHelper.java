package org.apache.skywalking.apm.plugin;

import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;

/**
 * @author no-today
 * @date 2022/10/27 09:59
 */
class NettyRemotingHelper {

    static final StringTag TAG_REQUEST = new StringTag("Request");
    static final StringTag TAG_RESPONSE = new StringTag("Response");

    static final StringTag TAG_LOGIN = new StringTag("Login");
    static final StringTag TAG_PROCESSOR = new StringTag("Processor");
    static final StringTag TAG_RESPONSE_CODE = new StringTag("ResponseCode");

    static String generateOperationName(RemotingCommand command) {
        return "netty-remoting/" + command.getCode();
    }
}
