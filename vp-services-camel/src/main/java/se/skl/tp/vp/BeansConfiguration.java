package se.skl.tp.vp;

import io.netty.channel.EventLoopGroup;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.component.netty.NettyWorkerPoolBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import se.skl.tp.vp.logging.EcsMessageLogger;
import se.skl.tp.vp.logging.MessageLogger;
import se.skl.tp.vp.logging.old.LegacyMessageInfoLogger;

@Configuration
@ComponentScan(basePackages = {"se.skltp.takcache", "se.skl.tp.hsa.cache", "se.skl.tp.behorighet", "se.skl.tp.vagval", "se.skl.tp.vp"})
@Log4j2
public class BeansConfiguration {

  @Value("${producer.http.workers}")
  private int httpWorkers;

  @Value("${producer.https.workers}")
  private int httpsWorkers;

  @Bean
  public EventLoopGroup sharedClientHttpPool(){
    return new NettyWorkerPoolBuilder().withWorkerCount(httpWorkers).withName("NettyHttpClient").build();
  }

  @Bean
  public EventLoopGroup sharedClientHttpsPool(){
    return new NettyWorkerPoolBuilder().withWorkerCount(httpsWorkers).withName("NettyHttpsClient").build();
  }

  @Bean
  @ConditionalOnProperty(name = "vp.logging.style", havingValue = "ECS")
  public MessageLogger ecsMessageLogger() {
    log.debug("Configuring ECS format message logger");
    return new EcsMessageLogger();
  }

  @Bean
  @ConditionalOnProperty(name = "vp.logging.style", havingValue = "LEGACY", matchIfMissing = true)
  @SuppressWarnings({"removal"}) // LegacyMessageInfoLogger is deprecated, but supported for backward compatibility until removed in a future version.
  public MessageLogger legacyMessageInfoLogger() {
    log.warn("Configuring LEGACY format message logger. This format is deprecated. Please migrate to ECS format by setting vp.logging.style=ECS");
    return new LegacyMessageInfoLogger();
  }
}
