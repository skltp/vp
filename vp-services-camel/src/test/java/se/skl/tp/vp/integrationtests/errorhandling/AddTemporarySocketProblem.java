package se.skl.tp.vp.integrationtests.errorhandling;

import static se.skl.tp.vp.VPRouter.TO_PRODUCER_ROUTE;

import java.net.SocketException;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class AddTemporarySocketProblem extends RouteBuilder implements Processor {

  public static final String BODY_ON_SECOND_INVOKATION = "<anybody><message>re-sent</message></anybody>";

  //Config
  private String regExpOrUrl;
  private String urlMockEndpoint;


  private int reSendAttempts = 0;
  private int maxException =1;


  @Override
  public void configure() {
    interceptSendToEndpoint(regExpOrUrl)
        .skipSendToOriginalEndpoint()
        .process(this)
        .to(urlMockEndpoint);
  }

  /*
  public static void toProducerOnProducerRoute(CamelContext destination,String urlMockEndpoint) throws Exception {
    destination.getRouteDefinition(TO_PRODUCER_ROUTE)
        .adviceWith(
            destination,
            new AddTemporarySocketProblem(
                  ".*localhost:12126.*",
                //"mock_producer_address",
                urlMockEndpoint,1)
        );
  }
*/

  public AddTemporarySocketProblem(String interceptionUrlOrRegEx,
      String urlMockEndpoint,int maxNoOfProblem){
    this.regExpOrUrl = interceptionUrlOrRegEx;
    this.urlMockEndpoint = urlMockEndpoint;
    this.maxException = maxNoOfProblem;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    if (shouldThrowException()) {
      throw new SocketException("Simulated Error");
    } else {
      exchange.getOut().setBody(BODY_ON_SECOND_INVOKATION);
    }
  }

  private boolean shouldThrowException() {
    boolean result = reSendAttempts < maxException;
    if (result) {
      reSendAttempts++;
    }
    return result;
  }

}
