package se.skl.tp.vp.integrationtests.errorhandling;

import static se.skl.tp.vp.VPRouter.TO_PRODUCER_ROUTE;

import java.net.SocketException;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWith;

/**
 * Should this class be deprecated. Does not seem to be in use. /hanwik
 *
 */
public class AddTemporarySocketProblem implements Processor {

  public static final String BODY_ON_SECOND_INVOKATION = "<anybody><message>re-sent</message></anybody>";

  private int reSendAttempts = 0;
  private int maxException =1;

  public void toProducerOnProducerRoute(CamelContext destination,String urlMockEndpoint) throws Exception {

	  AdviceWith.adviceWith(destination, TO_PRODUCER_ROUTE, a -> {
		  a.interceptSendToEndpoint(".*localhost:12126.*")
		  	.skipSendToOriginalEndpoint()
		  	.process(this)
		  	.to(urlMockEndpoint);
		}
	  );  
  }

  public AddTemporarySocketProblem() {}
  
  public AddTemporarySocketProblem(String interceptionUrlOrRegEx,
      String urlMockEndpoint,int maxNoOfProblem){
    this.maxException = maxNoOfProblem;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    if (shouldThrowException()) {
      throw new SocketException("Simulated Error");
    } else {
      exchange.getMessage().setBody(BODY_ON_SECOND_INVOKATION);
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
