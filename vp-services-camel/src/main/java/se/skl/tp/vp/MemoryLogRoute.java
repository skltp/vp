package se.skl.tp.vp;

import static se.skl.tp.vp.utils.MemoryUtil.getNettyMemoryJsonString;

import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.constants.PropertyConstants;

@Component
@Log4j2(topic="se.skl.tp.vp.logging.memory")
public class MemoryLogRoute extends RouteBuilder {

  @Value("${" + PropertyConstants.MEMORY_LOG_PERIOD + "}")
  int periodInSeconds;

  @Override
  public void configure() throws Exception {
    from("timer://memoryLogger?fixedRate=true&delay=120000&period="+periodInSeconds*1000).routeId("LoggerRoute")
        .process((Exchange exchange) -> log.info(getNettyMemoryJsonString()));
  }
}
