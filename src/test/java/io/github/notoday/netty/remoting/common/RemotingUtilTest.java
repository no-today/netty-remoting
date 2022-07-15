package io.github.notoday.netty.remoting.common;

import junit.framework.TestCase;

/**
 * @author no-today
 * @date 2022/07/01 20:44
 */
public class RemotingUtilTest extends TestCase {

    public void testGetLocalAddress() {
        System.out.println(RemotingUtil.getLocalAddress());
    }
}