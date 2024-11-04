package se.skl.tp.vp.hawtioauth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jetty.security.jaas.spi.AbstractLoginModule;
import org.eclipse.jetty.security.PropertyUserStore;
import org.eclipse.jetty.security.RolePrincipal;
import org.eclipse.jetty.security.UserPrincipal;
import org.eclipse.jetty.util.resource.PathResourceFactory;

@Log4j2
public class PropertyFileLoginModule extends AbstractLoginModule {

    private static ConcurrentHashMap<String, PropertyUserStore> PROPERTY_USERSTORES =
            new ConcurrentHashMap<>();

    private int reloadInterval = 0;
    private String filename = null;

    /**
     * Read contents of the configured property file.
     * Only the callback is used in this case.
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
        setupPropertyUserStore();
    }

    private void setupPropertyUserStore() {
        parseConfig();
        if (PROPERTY_USERSTORES.get(filename) == null) {
            final PropertyUserStore propertyUserStore = new PropertyUserStore();
            propertyUserStore.setConfig(new PathResourceFactory().newResource(filename));
            propertyUserStore.setReloadInterval(reloadInterval);

            final PropertyUserStore prev = PROPERTY_USERSTORES.putIfAbsent(filename, propertyUserStore);
            if (prev == null) {
                log.info("setupPropertyUserStore: Starting new PropertyUserStore. PropertiesFile: "
                        + filename + " reloadInterval: " + reloadInterval);
                try {
                    propertyUserStore.start();
                } catch (Exception e) {
                    log.warn("Exception while starting propertyUserStore: ", e);
                }
            }
        }
    }

    private void parseConfig() {
        filename = System.getProperty("hawtiologin.file", filename);
        reloadInterval = 0;
    }

    @Override
    public JAASUser getUser(String userName) throws Exception {
        final PropertyUserStore propertyUserStore = PROPERTY_USERSTORES.get(filename);
        if (propertyUserStore == null) {
            throw new IllegalStateException("PropertyUserStore should never be null here!");
        }

        log.debug("Checking PropertyUserStore " + filename + " for " + userName);
        UserPrincipal userPrincipal = propertyUserStore.getUserPrincipal(userName);
        if (userPrincipal == null) {
            log.error("No user principal found in external login file.");
            return null;
        }

        List<RolePrincipal> rps = propertyUserStore.getRolePrincipals(userName);
        final List<String> roles = rps == null ? Collections.emptyList()
                : rps.stream().map(RolePrincipal::getName).collect(Collectors.toList());
        return new JAASUser(userPrincipal) {
            public List<String> doFetchRoles() {
                return roles;
            }
        };
    }

    @Override
    public boolean logout() throws LoginException {
        return true;
    }
}
