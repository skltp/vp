package se.skl.tp.vp.integrationtests.errorhandling.connectionResetByPeer;

import io.netty.channel.ChannelHandlerContext;

@FunctionalInterface
public interface ServerBehavior {
    void work(ChannelHandlerContext ctx);
}
