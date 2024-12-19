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
  private String allowedIncomingCipherSuites;
  private String allowedOutgoingCipherSuites;
  private Store store;

  @Data
  public static class Store {
    private String location;
    private SSLConfig producer;
    private SSLConfig consumer;
    private Truststore truststore;

    public String getLocation() {
      // Set file scheme if not specified (for backwards compatibility)
      return location.contains(":") ? location : "file:" + location;
    }

    @Data
    public static class SSLConfig {
      private String file;
      private String password;
      private String keyPassword;
      private String bundle;
    }

    @Data
    public static class Truststore {
      private String file;
      private String password;
    }
  }

}
