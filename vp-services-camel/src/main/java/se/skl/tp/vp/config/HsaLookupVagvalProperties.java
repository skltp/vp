package se.skl.tp.vp.config;

import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import se.skl.tp.HsaLookupConfiguration;

@Data
@NoArgsConstructor
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "hsa.lookup.vagval")
public class HsaLookupVagvalProperties implements HsaLookupConfiguration {
  /**
   * Is HSA lookup active by default (for routing)?
   */
  private boolean defaultEnabled = true;

  /**
   * Exceptions to the default setting for certain service contract namespaces.
   */
  private List<String> exceptedNamespaces = Collections.emptyList();
}
