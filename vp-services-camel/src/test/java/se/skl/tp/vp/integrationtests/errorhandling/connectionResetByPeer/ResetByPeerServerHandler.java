package se.skl.tp.vp.integrationtests.errorhandling.connectionResetByPeer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

public class ResetByPeerServerHandler extends SimpleChannelInboundHandler<Object> {

    private ServerBehavior behavior;

    public ResetByPeerServerHandler() {
    }

    public ResetByPeerServerHandler(ServerBehavior behavior) {
        this.behavior = behavior;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            behavior.work(ctx);
        }
    }
}



