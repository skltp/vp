package se.skl.tp.vp.hawtioauth;

import io.hawt.springboot.ConditionalOnExposedEndpoint;
import io.hawt.web.auth.AuthenticationConfiguration;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import se.skl.tp.vp.constants.PropertyConstants;

@Log4j2
@Configuration
@ConditionalOnExposedEndpoint(name = "hawtio")
public class HawtioConfiguration {

  private static final String JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";

  @Value("${" + AuthenticationConfiguration.HAWTIO_AUTHENTICATION_ENABLED + ":#{false}}")
  private Boolean hawtioAuthenticationEnabled;

  @Value("${" + PropertyConstants.HAWTIO_EXTERNAL_LOGINFILE + ":#{null}}")
  private String externalLoginFile;

  /**
   * Configure hawtio authentication.
   */
  @PostConstruct
  public void init() {
    log.info("Configuring authentication for Hawtio: hawtioAuthenticationEnabled is " + hawtioAuthenticationEnabled);
    if (!hawtioAuthenticationEnabled) {
      log.warn("Authentication set to false for Hawtio. NOT recommended.");
    } else {
      final URL loginResource = this.getClass().getClassLoader().getResource("login.conf");
      if (loginResource != null) {
        setSystemPropertyIfNotSet(JAVA_SECURITY_AUTH_LOGIN_CONFIG, loginResource.toExternalForm());
        log.info("Hawtio: Using loginResource " + JAVA_SECURITY_AUTH_LOGIN_CONFIG + " : "
            + System.getProperty(JAVA_SECURITY_AUTH_LOGIN_CONFIG));
        setLoginFile();
      } else {
        log.error("Hawtio: Login resource (login.conf) for setting system property " + JAVA_SECURITY_AUTH_LOGIN_CONFIG + " was null!");
      }
    }
  }

  private void setLoginFile() {
    URL loginFileUrl;
    if (externalLoginFile != null) {
      File f = new File(externalLoginFile);
      if (f.exists() && f.isFile() && f.canRead()) {
        try {
          loginFileUrl = f.toURI().toURL();
          setSystemPropertyIfNotSet("hawtiologin.file", loginFileUrl.toExternalForm());
          setSystemPropertyIfNotSet(AuthenticationConfiguration.HAWTIO_ROLES, "user");
          setSystemPropertyIfNotSet(AuthenticationConfiguration.HAWTIO_ROLES, "admin");
          setSystemPropertyIfNotSet(AuthenticationConfiguration.HAWTIO_REALM, "hawtio");
          setSystemPropertyIfNotSet(
              AuthenticationConfiguration.HAWTIO_ROLE_PRINCIPAL_CLASSES,
              "org.eclipse.jetty.jaas.JAASRole");
          log.info("Hawtio: Using loginfile : " + loginFileUrl);

        } catch (MalformedURLException mue) {
          log.error("Hawtio: The external loginFile URL is malformed. URI was " + f.toURI() + " Hawtio is NOT accessible.\n" + mue.getMessage());
        }
      } else {
        log.error("Hawtio: The external loginFile was not found or not readable. Path is " + f.getAbsolutePath());
      }
    } else {
      log.error("Hawtio: The property " + PropertyConstants.HAWTIO_EXTERNAL_LOGINFILE + " is NOT set. It is mandatory when property " +
          PropertyConstants.HAWTIO_AUTHENTICATION_ENABLED + " is set to true.");
    }
  }

  private void setSystemPropertyIfNotSet(final String key, final String value) {
    if (System.getProperty(key) == null) {
      System.setProperty(key, value);
    }
  }
}
