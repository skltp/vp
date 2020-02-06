package se.skl.tp.vp.config;

import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import se.skl.tp.DefaultRoutingConfiguration;

@Data
@NoArgsConstructor
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "defaultrouting")
public class DefaultRoutingProperties implements DefaultRoutingConfiguration {

  /**
   * The delimiter for the deprecated old style routing VE#VG. If empty the default routing is disabled
   */
  private String delimiter = "#";

  /**
   * Servicecontracts allowed to be used with old style default routing
   */
  private List<String> allowedContracts = Collections.emptyList();

  /**
   * Consumer ids allowed to use old style default routing
   */
  private List<String> allowedSenderIds = Collections.emptyList();
}
