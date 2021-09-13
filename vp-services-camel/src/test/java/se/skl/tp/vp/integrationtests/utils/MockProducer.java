package se.skl.tp.vp.integrationtests.utils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import se.skl.tp.vp.constants.HttpHeaders;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Data
@Component
@Log4j2
public class MockProducer {
  public static final String NETTY_HTTP = "netty-http:";

  private Integer responseHttpStatus=200;
  private String responseBody="response text";
  private String responseResourceXml =null;
  private Integer timeout=0;

  private RandomCollection<Integer> weightedTimeouts = new RandomCollection<>();

  private CamelContext camelContext;

  String inBody;
  XMLStreamReader inBodyXmlReader;
  Map<String, Object> inHeaders = new HashMap<>();
  Map<String, Object> outHeaders = new HashMap<>();

  @Autowired
  public MockProducer(CamelContext camelContext){
    this.camelContext = camelContext;
  }

  public MockProducer(CamelContext camelContext, String producerAddress) throws Exception {
    this.camelContext = camelContext;
    start(producerAddress);
  }


  public void start(String producerAddress) throws Exception {
    inHeaders.clear();
    outHeaders.clear();
    inBody=null;
    Route route = camelContext.getRoute(producerAddress);
    if(route!=null){
      log.info("Producer route with address '{}' already started", producerAddress);
      return;
    }

    camelContext.addRoutes(new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from(NETTY_HTTP + producerAddress).id(producerAddress).routeDescription("Producer")
            .streamCaching()
            .process((Exchange exchange) -> {
              inHeaders.putAll(exchange.getIn().getHeaders());
              inBody = exchange.getIn().getBody(String.class);
              inBodyXmlReader = exchange.getIn().getBody(XMLStreamReader.class);
              if(responseResourceXml != null){
                addResponseFromFile(exchange, responseResourceXml);
              } else {
                exchange.getOut().setBody(responseBody);
              }
              addResponseHeader(Exchange.HTTP_RESPONSE_CODE, responseHttpStatus);
              updateRoutingHistory();
              exchange.getOut().setHeaders(outHeaders);
              final long timeoutValue = getTimeoutValue();
              Thread.sleep(timeoutValue);
            });
      }
    });
  }

  private long getTimeoutValue() {
    if(weightedTimeouts.isEmpty()){
      return timeout;
    }
    return weightedTimeouts.next();
  }

  private void updateRoutingHistory() {
	  String routinghistory = getInHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY);
	  if(routinghistory == null || routinghistory.length() == 0)
          addResponseHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY, "mock-producer");
	  else
          addResponseHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY, routinghistory +",mock-producer");		  
  }
  
  public String getInHeader(String header){
    return (String)inHeaders.get(header);
  }

  public void addResponseHeader(String key, Object value){
    outHeaders.put(key, value);
  }

  private void addResponseFromFile(Exchange exchange, String fileName) throws IOException, XMLStreamException {
    final URL resource = Thread.currentThread().getContextClassLoader().getResource(fileName);
    final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());
    exchange.getOut().setBody(xstream);
  }

}
