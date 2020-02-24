package se.skl.tp.vp.hawtioAuth;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jetty.jaas.spi.AbstractLoginModule;
import org.eclipse.jetty.jaas.spi.UserInfo;
import org.eclipse.jetty.security.PropertyUserStore;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

@Log4j2
public class PropertyFileLoginModule extends AbstractLoginModule {
  public static final String DEFAULT_FILENAME = "realm.properties";

  private static ConcurrentHashMap<String, PropertyUserStore> PROPERTY_USERSTORES =
      new ConcurrentHashMap<>();

  private boolean hotReload = false;
  private String filename = null;

  /**
   * Read contents of the configured property file.
   *
   * @param subject
   * @param callbackHandler
   * @param sharedState
   * @param options
   * @see javax.security.auth.spi.LoginModule#initialize(Subject, CallbackHandler, Map, Map)
   */
  @Override
  public void initialize(
      final Subject subject,
      final CallbackHandler callbackHandler,
      final Map<String, ?> sharedState,
      final Map<String, ?> options) {
    super.initialize(subject, callbackHandler, sharedState, options);
    setupPropertyUserStore(options);
  }

  private void setupPropertyUserStore(final Map<String, ?> options) {
    parseConfig(options);

    if (PROPERTY_USERSTORES.get(filename) == null) {
      final PropertyUserStore propertyUserStore = new PropertyUserStore();
      propertyUserStore.setConfig(filename);
      propertyUserStore.setHotReload(hotReload);

      final PropertyUserStore prev = PROPERTY_USERSTORES.putIfAbsent(filename, propertyUserStore);
      if (prev == null) {
        log.info("setupPropertyUserStore: Starting new PropertyUserStore. PropertiesFile: "
                + filename + " hotReload: " + hotReload);
        try {
          propertyUserStore.start();
        } catch (Exception e) {
          log.warn("Exception while starting propertyUserStore: ", e);
        }
      }
    }
  }

  private void parseConfig(final Map<String, ?> options) {
    String tmp = (String) options.get("file");
    filename = (tmp == null ? DEFAULT_FILENAME : tmp);
    filename = System.getProperty("login.file", filename);
    tmp = (String) options.get("hotReload");
    hotReload = tmp == null ? hotReload : Boolean.parseBoolean(tmp);
  }

  @Override
  public UserInfo getUserInfo(final String userName) {
    final PropertyUserStore propertyUserStore = PROPERTY_USERSTORES.get(filename);
    if (propertyUserStore == null) {
      throw new IllegalStateException("PropertyUserStore should never be null here!");
    }

    log.debug("Checking PropertyUserStore " + filename + " for " + userName);
    final UserIdentity userIdentity = propertyUserStore.getUserIdentity(userName);
    if (userIdentity == null) {
      return null;
    }

    final Set<Principal> principals = userIdentity.getSubject().getPrincipals();
    final List<String> roles = new ArrayList<>();
    for (final Principal principal : principals) {
      roles.add(principal.getName());
    }

    final Credential credential =
        (Credential) userIdentity.getSubject().getPrivateCredentials().iterator().next();
    log.debug("Found: " + userName + " in PropertyUserStore " + filename);
    return new UserInfo(userName, credential, roles);
  }

  @Override
  public boolean logout() throws LoginException {
    return true;
  }
}
