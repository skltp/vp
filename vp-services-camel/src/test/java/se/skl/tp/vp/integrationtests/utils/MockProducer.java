package se.skl.tp.vp.integrationtests.utils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.support.jsse.CipherSuitesParameters;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.SecureSocketProtocolsParameters;
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

  // Add SSL configuration fields
  private String[] enabledProtocols;
  private String[] enabledCipherSuites;

  // Server certificate configuration
  private String serverKeystoreResource = "certs/tp.jks";
  private String serverKeystorePassword = "password";
  private String serverKeystoreType = "JKS";

  // Client certificate verification configuration
  private static final String TRUSTED_CA_CERT_RESOURCE = "cert/truststore.jks";
  private static final String TRUSTED_CA_CERT_PASSWORD = "password";

  private RandomCollection<Integer> weightedTimeouts = new RandomCollection<>();

  private CamelContext camelContext;

  private String producerAddress;

  String inBody;
  XMLStreamReader inBodyXmlReader;
  Map<String, Object> inHeaders = new HashMap<>();
  Map<String, Object> outHeaders = new HashMap<>();

  // SSL session information
  @Getter
  private String lastNegotiatedProtocol;
  @Getter
  private String lastNegotiatedCipherSuite;

  @Autowired
  public MockProducer(CamelContext camelContext){
    this.camelContext = camelContext;
  }

  public MockProducer(CamelContext camelContext, String producerAddress) throws Exception {
    this.camelContext = camelContext;
    start(producerAddress);
  }

  public MockProducer withServerKeystore(String resource,  String password) {
    this.serverKeystoreResource = resource;
    this.serverKeystorePassword = password;
    // Detect keystore type from file extension
    if (resource != null && (resource.endsWith(".p12") || resource.endsWith(".pfx"))) {
      this.serverKeystoreType = "PKCS12";
    } else {
      this.serverKeystoreType = "JKS";
    }
    return this;
  }

  public MockProducer withProtocols(String... protocols) {
    this.enabledProtocols = protocols;
    return this;
  }

  public MockProducer withCipherSuites(String... cipherSuites) {
    this.enabledCipherSuites = cipherSuites;
    return this;
  }

  @SuppressWarnings("java:S2925") // Sleep is needed to simulate timeout
  public void start(String producerAddress) throws Exception {
    this.producerAddress = producerAddress;
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
      public void configure() {
        StringBuilder endpoint = new StringBuilder(NETTY_HTTP).append(producerAddress);

        if (producerAddress.startsWith("https")) {
          SSLContextParameters sslParams = createSslContextParameters();
          String paramName = "mockProducerSSL-" + producerAddress.hashCode();
          getContext().getRegistry().bind(paramName, sslParams);
          endpoint.append("?sslContextParameters=#").append(paramName).append("&ssl=true");
        }

        from(endpoint.toString()).id(producerAddress).routeDescription("Producer")
            .process((Exchange exchange) -> {
              // Capture SSL session information if available
              try {
                javax.net.ssl.SSLSession sslSession = exchange.getIn().getHeader(
                    "CamelNettySSLSession", javax.net.ssl.SSLSession.class);
                if (sslSession != null) {
                  lastNegotiatedProtocol = sslSession.getProtocol();
                  lastNegotiatedCipherSuite = sslSession.getCipherSuite();
                  log.debug("Captured SSL session - Protocol: {}, CipherSuite: {}",
                      lastNegotiatedProtocol, lastNegotiatedCipherSuite);
                }
              } catch (Exception e) {
                log.debug("Could not retrieve SSL session information", e);
              }

              inHeaders.putAll(exchange.getIn().getHeaders());
              inBody = exchange.getIn().getBody(String.class);
              inBodyXmlReader = exchange.getIn().getBody(XMLStreamReader.class);
              if(responseResourceXml != null){
                addResponseFromFile(exchange, responseResourceXml);
              } else {
                exchange.getMessage().setBody(responseBody);
              }
              addResponseHeader(Exchange.HTTP_RESPONSE_CODE, responseHttpStatus);
              updateRoutingHistory();
              exchange.getMessage().setHeaders(outHeaders);
              final long timeoutValue = getTimeoutValue();
              Thread.sleep(timeoutValue);
            });
      }
    });
  }

  public void stop() throws Exception {
    Route route = camelContext.getRoute(producerAddress);
    if (route != null) {
      camelContext.getRouteController().stopRoute(producerAddress);
      camelContext.removeRoute(producerAddress);

      // Also unbind the SSL context parameters from registry to fully clean up
      if (producerAddress.startsWith("https")) {
        String paramName = "mockProducerSSL-" + producerAddress.hashCode();
        try {
          camelContext.getRegistry().unbind(paramName);
        } catch (Exception e) {
          // Ignore if not found
          log.debug("Could not unbind SSL parameters '{}': {}", paramName, e.getMessage());
        }
      }

      log.info("Producer route with address '{}' stopped and removed", producerAddress);
    }
  }

  private SSLContextParameters createSslContextParameters(){
    SSLContextParameters sslParams = new SSLContextParameters();

    try {
      if (serverKeystoreResource != null && serverKeystorePassword != null) {
        KeyStoreParameters keyStoreParams = new KeyStoreParameters();
        keyStoreParams.setResource("classpath:" + serverKeystoreResource);
        keyStoreParams.setPassword(serverKeystorePassword);
        keyStoreParams.setType(serverKeystoreType);

        KeyManagersParameters keyManagersParams = new KeyManagersParameters();
        keyManagersParams.setKeyStore(keyStoreParams);
        keyManagersParams.setKeyPassword(serverKeystorePassword);

        sslParams.setKeyManagers(keyManagersParams);
      }

      if (enabledProtocols != null) {
        SecureSocketProtocolsParameters protocols = new SecureSocketProtocolsParameters();
        protocols.setSecureSocketProtocol(java.util.Arrays.asList(enabledProtocols));
        sslParams.setSecureSocketProtocols(protocols);
      }

      if (enabledCipherSuites != null) {
        CipherSuitesParameters ciphers = new CipherSuitesParameters();
        ciphers.setCipherSuite(java.util.Arrays.asList(enabledCipherSuites));
        sslParams.setCipherSuites(ciphers);
      }

    } catch (Exception e) {
      log.error("Failed to configure SSL context parameters", e);
      throw new RuntimeException("Failed to configure SSL context parameters", e);
    }

    return sslParams;
  }

  private long getTimeoutValue() {
    if(weightedTimeouts.isEmpty()){
      return timeout;
    }
    return weightedTimeouts.next();
  }

  private void updateRoutingHistory() {
	  String routinghistory = getInHeader(HttpHeaders.X_RIVTA_ROUTING_HISTORY);
	  if(routinghistory == null || routinghistory.isEmpty())
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
    assert resource != null;
    final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());
    exchange.getMessage().setBody(xstream);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    MockProducer that = (MockProducer) o;
    return Objects.equals(getProducerAddress(), that.getProducerAddress());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getProducerAddress());
  }
}
