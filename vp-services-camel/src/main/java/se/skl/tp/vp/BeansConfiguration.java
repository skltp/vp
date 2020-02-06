package se.skl.tp.vp;

import io.netty.channel.EventLoopGroup;
import org.apache.camel.component.netty4.NettyWorkerPoolBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"se.skltp.takcache", "se.skl.tp.hsa.cache", "se.skl.tp.behorighet", "se.skl.tp.vagval", "se.skl.tp.vp"})
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

}
