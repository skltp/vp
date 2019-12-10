package se.skl.tp.vp.integrationtests.errorhandling.connectionResetByPeer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ResetByPeerServer {
    private static Channel channel;

    private static EventLoopGroup bossGroup;
    private static EventLoopGroup workerGroup;


    public static void startServer(int port, ServerBehavior behavior) throws InterruptedException {
        // Configure the server.
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.childOption(ChannelOption.SO_LINGER, 0);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ResetByPeerServerInitializer(null, behavior));

            channel = b.bind(port).sync().channel();

        } finally {

        }
    }

    public static void stopServer(){
        channel.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

}
