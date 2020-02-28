package se.skl.tp.vp.hawtioauth;

import io.hawt.config.ConfigFacade;
import io.hawt.web.auth.AuthenticationConfiguration;
import java.net.URL;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.skl.tp.vp.constants.PropertyConstants;

@Log4j2
@Configuration
public class HawtioConfiguration {

  private static final String JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";

  @Value("${" + PropertyConstants.HAWTIO_AUTHENTICATION_ENABLED + ":#{true}}")
  private Boolean hawtioAuthenticationEnabled;

  /**
   * Configure facade to use/not use authentication.
   *
   * @return config
   * @throws Exception if an error occurs
   */
  @Bean(initMethod = "init")
  public ConfigFacade configFacade() throws Exception {
    if (!hawtioAuthenticationEnabled) {
      System.setProperty(AuthenticationConfiguration.HAWTIO_AUTHENTICATION_ENABLED, "false");
    } else {
      final URL loginResource = this.getClass().getClassLoader().getResource("login.conf");
      if (loginResource != null) {
        setSystemPropertyIfNotSet(JAVA_SECURITY_AUTH_LOGIN_CONFIG, loginResource.toExternalForm());
      }
      log.info("Using loginResource " + JAVA_SECURITY_AUTH_LOGIN_CONFIG + " : "
              + System.getProperty(JAVA_SECURITY_AUTH_LOGIN_CONFIG));

      final URL loginFile = this.getClass().getClassLoader().getResource("realm.properties");
      if (loginFile != null) {
        setSystemPropertyIfNotSet("login.file", loginFile.toExternalForm());
      }
      log.info("Using login.file : " + System.getProperty("login.file"));

      setSystemPropertyIfNotSet(AuthenticationConfiguration.HAWTIO_ROLES, "user");
      setSystemPropertyIfNotSet(AuthenticationConfiguration.HAWTIO_ROLES, "admin");
      setSystemPropertyIfNotSet(AuthenticationConfiguration.HAWTIO_REALM, "hawtio");
      setSystemPropertyIfNotSet(
          AuthenticationConfiguration.HAWTIO_ROLE_PRINCIPAL_CLASSES,
          "org.eclipse.jetty.jaas.JAASRole");
      System.setProperty(AuthenticationConfiguration.HAWTIO_AUTHENTICATION_ENABLED, "true");
    }
    return new ConfigFacade();
  }

  private void setSystemPropertyIfNotSet(final String key, final String value) {
    if (System.getProperty(key) == null) {
      System.setProperty(key, value);
    }
  }
}
