package se.skl.tp.vp;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.vagval.ResetTakCacheProcessor;

@Component
public class ResetTakCacheRouter extends RouteBuilder {

  public static final String RESET_TAK_CACHE_ROUTE = "reset-tak-cache-route";
  public static final String NETTY4_HTTP_FROM_RESET_TAK_CACHE = "netty4-http:{{vp.reset.cache.url}}";

  @Autowired
  ResetTakCacheProcessor resetTakCacheProcessor;

  @Override
  public void configure() throws Exception {
    from(NETTY4_HTTP_FROM_RESET_TAK_CACHE).routeId(RESET_TAK_CACHE_ROUTE)
        .process(resetTakCacheProcessor);
  }
}
