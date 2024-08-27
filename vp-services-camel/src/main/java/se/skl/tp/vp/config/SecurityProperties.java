package se.skl.tp.vp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tp.tls")
public class SecurityProperties {

  private String allowedIncomingProtocols;
  private String allowedOutgoingProtocols;
  private String preferredOutgoingProtocol;
  private String allowedIncomingCipherSuites;
  private String allowedOutgoingCipherSuites;
  private Store store;

  @Data
  public static class Store {
    private String location;
    private Producer producer;
    private Consumer consumer;
    private Truststore truststore;

    @Data
    public static class Producer {
      private String file;
      private String password;
      private String keyPassword;
    }

    @Data
    public static class Consumer {
      private String file;
      private String password;
      private String keyPassword;
    }

    @Data
    public static class Truststore {
      private String file;
      private String password;
    }

  }

}
