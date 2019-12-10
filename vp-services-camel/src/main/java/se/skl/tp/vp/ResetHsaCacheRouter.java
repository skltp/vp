package se.skl.tp.vp;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.vagval.ResetHsaCacheProcessor;

@Component
public class ResetHsaCacheRouter extends RouteBuilder {

  public static final String RESET_HSA_CACHE_ROUTE = "reset-hsa-cache-route";
  public static final String NETTY4_HTTP_FROM_RESET_HSA_CACHE = "netty4-http:{{vp.hsa.reset.cache.url}}";

  @Autowired
  ResetHsaCacheProcessor resetHsaCacheProcessor;

  @Override
  public void configure() throws Exception {
    from(NETTY4_HTTP_FROM_RESET_HSA_CACHE).routeId(RESET_HSA_CACHE_ROUTE)
        .process(resetHsaCacheProcessor);
  }
}
