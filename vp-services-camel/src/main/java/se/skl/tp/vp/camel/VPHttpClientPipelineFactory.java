package se.skl.tp.vp.camel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.component.netty.ClientInitializerFactory;
import org.apache.camel.component.netty.NettyProducer;
import org.apache.camel.component.netty.http.NettyHttpConfiguration;
import org.apache.camel.component.netty.http.NettyHttpProducer;
import org.apache.camel.component.netty.http.handlers.HttpOutboundStreamHandler;
import org.apache.camel.util.ObjectHelper;
import org.springframework.stereotype.Component;

/*
This is a override of HttpClientInitializerFactory class from
camel-netty-http component.To configure the netty-http component
to use this class use the "clientInitializerFactory" configuration option.

The reason for overriding camel default client initializer
is to implement handling of the TLS SNI extension.
 */
@Log4j2
@Component
public class VPHttpClientPipelineFactory extends ClientInitializerFactory {

  protected NettyHttpConfiguration configuration;
  protected NettyHttpProducer nettyProducer;
  protected SSLContext sslContext;

  public VPHttpClientPipelineFactory() {
    // default constructor needed
  }

  public VPHttpClientPipelineFactory(NettyHttpProducer nettyProducer) {
    this.nettyProducer = nettyProducer;
    configuration = nettyProducer.getConfiguration();

    if (configuration.isSsl()) {
      try {
        this.sslContext = createSSLContext(nettyProducer);
        log.info("Created SslContext {}", sslContext);
      } catch (Exception e) {
        throw ObjectHelper.wrapRuntimeCamelException(e);
      }
    }
  }

  @Override
  public ClientInitializerFactory createPipelineFactory(NettyProducer nettyProducer) {
    return new VPHttpClientPipelineFactory((NettyHttpProducer) nettyProducer);
  }

  @Override
  protected void initChannel(Channel ch) throws URISyntaxException {
    // create a new pipeline
    ChannelPipeline pipeline = ch.pipeline();

    if (configuration.isSsl()) {
      SslHandler sslHandler = configureClientSSLOnDemand();
      log.debug("Client SSL handler configured and added as an interceptor against the ChannelPipeline: {}", sslHandler);
      pipeline.addLast("ssl", sslHandler);
    }

    pipeline.addLast("http", new HttpClientCodec());
    pipeline.addLast("aggregator", new HttpObjectAggregator(configuration.getChunkedMaxContentLength()));
    pipeline.addLast("outbound-streamer", new HttpOutboundStreamHandler());


    if (configuration.getRequestTimeout() > 0) {
      if (log.isTraceEnabled()) {
        log.trace("Using request timeout {} millis", configuration.getRequestTimeout());
      }
      ChannelHandler timeout = new ReadTimeoutHandler(configuration.getRequestTimeout(), TimeUnit.MILLISECONDS);
      pipeline.addLast("timeout", timeout);
    }

    // handler to route Camel messages
    pipeline.addLast("handler", new HttpClientChannelHandler(nettyProducer));
  }

  private SSLContext createSSLContext(NettyProducer producer) throws GeneralSecurityException, IOException {

    if (configuration.getSslContextParameters() == null) {
      log.error("No getSslContextParameters configured for this ssl connection");
      return null;
    }

    return configuration.getSslContextParameters().createSSLContext(producer.getContext());
  }

  private SslHandler configureClientSSLOnDemand() throws URISyntaxException {

    if (sslContext != null) {
      URI uri = new URI(nettyProducer.getEndpoint().getEndpointUri());
      SSLEngine sllEngine = sslContext.createSSLEngine(uri.getHost(), uri.getPort());
      sllEngine.setUseClientMode(true);
      SSLParameters sslParameters = sllEngine.getSSLParameters();
      sslParameters.setServerNames(Arrays.asList(new SNIHostName(uri.getHost())));
      sllEngine.setSSLParameters(sslParameters);

      //TODO must close sslHandler on SSL exception
      //
      // The lines above was commented in the original Camel
      // initializer so I leave it here to be checked later (gerkstam)
      return new SslHandler(sllEngine);
    }

    return null;
  }


}
