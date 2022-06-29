package se.skl.tp.vp.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Note that the values are the header names/key used by the proxy server to forward the "actual
 * values" (this takes height for installing VP behind a proxy using non standard names/key)
 */
@Data
@NoArgsConstructor
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "http.forwarded.header")
public class ProxyHttpForwardedHeaderProperties {

  String xfor;

  String host;

  String port;

  String proto;

  String auth_cert;
}
