package io.github.notoday.netty.remoting.core;

import io.github.notoday.netty.remoting.protocol.RemotingCommand;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author no-today
 * @date 2022/05/31 16:01
 */
@Slf4j
public class RequestTask implements Runnable {

    private final Runnable runnable;
    private final Channel channel;
    private final RemotingCommand request;
    private final long createTimestamp = System.currentTimeMillis();
    private boolean stopRun = false;

    public RequestTask(Runnable runnable, Channel channel, RemotingCommand request) {
        this.runnable = runnable;
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void run() {
        if (stopRun) return;
        runnable.run();
    }

    public boolean isStopRun() {
        return stopRun;
    }

    public void setStopRun(boolean stopRun) {
        this.stopRun = stopRun;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public Channel getChannel() {
        return channel;
    }

    public RemotingCommand getRequest() {
        return request;
    }

    public long getCreateTimestamp() {
        return createTimestamp;
    }
}
