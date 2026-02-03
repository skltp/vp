package se.skl.tp.vp.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "vp.tls")
public class TLSProperties {
    @Data
    public static class TLSConfig {
        private String name;
        private String bundle;
        private List<String> protocolsInclude;
        private List<String> protocolsExclude;
        private List<String> cipherSuitesInclude;
        private List<String> cipherSuitesExclude;
    }

    @Data
    public static class TLSConfigMatch {
        private String domainName;
        private String domainSuffix;
        private Integer port;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class TLSOverride extends TLSConfig {
        private TLSConfigMatch match;
    }

    private TLSConfig defaultConfig;
    private List<TLSOverride> overrides;

    /**
     * Enable mTLS verification for outgoing HTTPS connections.
     */
    private boolean mtlsVerificationEnabled = false;
}
