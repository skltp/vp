package se.skl.tp.vp.integrationtests.utils;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.constants.PropertyConstants;

@Component
public class TestConsumer {
  public static final String DIRECT_START_HTTP = "direct:start_http";
  public static final String DIRECT_START_HTTPS = "direct:start_https";
  private static final String NETTY_PREFIX = "netty-http:";
  public static final String HTTPS_NETTY_OPTIONS = "sslContextParameters=#outgoingSSLContextParameters&ssl=true&throwExceptionOnFailure=false";
  public static final String HTTP_NETTY_OPTIONS = "throwExceptionOnFailure=false";

  private String httpConsumerRouteUrl;
  private String httpsConsumerRouteUrl;

  @Produce(uri = "direct:start")
  protected ProducerTemplate template;

  @EndpointInject(uri = "mock:result")
  protected MockEndpoint resultEndpoint;

  @Value("${vp.instance.id}")
  String vpInstanceId;

  private Environment env;

  @Autowired
  public TestConsumer(CamelContext camelContext, Environment env) throws Exception {
    this.env = env;
    createConsumerRouteUrls();
    createConsumerRoutes(camelContext);
  }

  public MockEndpoint getResultEndpoint(){
    return resultEndpoint;
  }

  public String getReceivedHeader(String headerName){
    if(resultEndpoint.getReceivedExchanges().size() > 0) {
      return resultEndpoint.getReceivedExchanges().get(0).getIn().getHeader(headerName, String.class);
    }
    return null;
  }

  public String sendHttpRequestToVP(String message, Map<String, Object> headers){
    return sendHttpRequestToVP(message, headers, true);
  }


  public String sendHttpRequestToVP(String message, Map<String, Object> headers, boolean reset){
    if(reset){
      resultEndpoint.reset();
    }
    return template.requestBodyAndHeaders(
            DIRECT_START_HTTP,
            message,
            headers, String.class
    );
  }

  public String sendHttpRequestToVP(String message){
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, "tp");
    resultEndpoint.reset();
    return template.requestBodyAndHeaders(
        DIRECT_START_HTTP,
        message,
        headers, String.class
    );
  }


  public byte[] sendHttpRequestToVP(byte[] message, Map<String, Object> headers){
    resultEndpoint.reset();
    return template.requestBodyAndHeaders(
        DIRECT_START_HTTP,
        message,
        headers, byte[].class
    );
  }

  public String sendHttpsRequestToVP(String message, Map<String, Object> headers){
    resultEndpoint.reset();
    return template.requestBodyAndHeaders(
            DIRECT_START_HTTPS,
            message,
            headers, String.class
    );
  }

  public String sendHttpRequestToVP(String path, String message, Map<String, Object> headers){
    String vpHttpBaseUrl = env.getProperty(PropertyConstants.VP_HTTP_ROUTE_URL);
    path = path.startsWith("/") ? path.substring(1) : path;
    String delimiter = path.contains("?") ? "&" : "?";
    String endpointUri = String.format("netty-http:%s/%s%s%s", vpHttpBaseUrl, path, delimiter, HTTP_NETTY_OPTIONS);

    resultEndpoint.reset();
    return template.requestBodyAndHeaders(
        endpointUri,
        message,
        headers, String.class
    );
  }

  public String sendHttpsRequestToVP(String path, String message, Map<String, Object> headers){
    String vpHttpsBaseUrl = env.getProperty(PropertyConstants.VP_HTTPS_ROUTE_URL);
    path = path.startsWith("/") ? path.substring(1) : path;
    String delimiter = path.contains("?") ? "&" : "?";
    String endpointUri = String.format("netty-http:%s/%s%s%s", vpHttpsBaseUrl, path, delimiter, HTTPS_NETTY_OPTIONS);

    resultEndpoint.reset();
    return template.requestBodyAndHeaders(
        endpointUri,
        message,
        headers, String.class
    );
  }

  public byte[] sendHttpsRequestToVP(byte[] message, Map<String, Object> headers){
    resultEndpoint.reset();
    return template.requestBodyAndHeaders(
        DIRECT_START_HTTPS,
        message,
        headers, byte[].class
    );
  }

  private void createConsumerRouteUrls(){
    String vpHttpBaseUrl = env.getProperty(PropertyConstants.VP_HTTP_ROUTE_URL);
    String vpHttpsBaseUrl = env.getProperty(PropertyConstants.VP_HTTPS_ROUTE_URL);
    httpConsumerRouteUrl = NETTY_PREFIX + vpHttpBaseUrl + "/PATH1 "+ "?"+ HTTP_NETTY_OPTIONS;
    httpsConsumerRouteUrl = NETTY_PREFIX + vpHttpsBaseUrl + "/PATH1 " + "?"+ HTTPS_NETTY_OPTIONS;
  }

  private void createConsumerRoutes(CamelContext camelContext) throws Exception {

    camelContext.addRoutes(new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        from(DIRECT_START_HTTP)
                .to(httpConsumerRouteUrl)
                .to("mock:result");

        from(DIRECT_START_HTTPS)
                .to(httpsConsumerRouteUrl)
                .to("mock:result");

      }
    });
  }

}
