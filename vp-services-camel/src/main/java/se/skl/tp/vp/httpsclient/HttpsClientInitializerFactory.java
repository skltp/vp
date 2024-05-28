package se.skl.tp.vp.httpsclient;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslHandler;
import org.apache.camel.component.netty.ClientInitializerFactory;
import org.apache.camel.component.netty.NettyProducer;
import org.apache.camel.component.netty.http.HttpClientInitializerFactory;
import org.apache.camel.component.netty.http.NettyHttpProducer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

public class HttpsClientInitializerFactory extends HttpClientInitializerFactory {

    public HttpsClientInitializerFactory() {
    }

    public HttpsClientInitializerFactory(NettyHttpProducer nettyProducer) {
        super(nettyProducer);
    }

    public ClientInitializerFactory createPipelineFactory(NettyProducer nettyProducer) {
        return new HttpsClientInitializerFactory((NettyHttpProducer)nettyProducer);
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        super.initChannel(ch);
        if (configuration.isHostnameVerification()) {
            ChannelHandler handler = ch.pipeline().get("ssl");
            SSLEngine engine = ((SslHandler)handler).engine();
            SSLParameters sslParams = engine.getSSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            engine.setSSLParameters(sslParams);
        }
    }
}
