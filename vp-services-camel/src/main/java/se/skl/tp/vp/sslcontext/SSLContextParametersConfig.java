package se.skl.tp.vp.sslcontext;

import jakarta.annotation.PostConstruct;
import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import se.skl.tp.vp.config.SecurityProperties;
import se.skl.tp.vp.config.TLSProperties;


import javax.net.ssl.*;

@Log4j2
@Configuration
public class SSLContextParametersConfig {

    public static final String DELIMITER = ",";
    public static final String DEPRECATED_CONTEXT = "default";

    private final SecurityProperties securityProperies;
    private final CamelContext camelContext;
    private final SslBundles sslBundles;
    private final TLSProperties tlsProperties;

    @Value("${tp.tls.store.consumer.bundle:}") String deprecatedBundle;

    public SSLContextParametersConfig(SecurityProperties securityProperies, CamelContext camelContext, SslBundles sslBundles, TLSProperties tlsProperties) {
        this.securityProperies = securityProperies;
        this.camelContext = camelContext;
        this.sslBundles = sslBundles;
        this.tlsProperties = tlsProperties;
    }

    public static @NonNull String getId(String name) {
        return "SSLContext-" + name;
    }

    @PostConstruct
    public void registerSSLContextParameters() throws GeneralSecurityException, IOException {
        if (tlsProperties.getDefaultConfig() != null) {
            validateUniqueConfigurationNames();

            SSLContextParameters defaultParams = createSSLContextParameters(tlsProperties.getDefaultConfig());
            camelContext.getRegistry().bind(getId(tlsProperties.getDefaultConfig().getName()), defaultParams);

            if (tlsProperties.getOverrides() != null) {
                for (var override : tlsProperties.getOverrides()) {
                    SSLContextParameters overrideParams = createSSLContextParameters(override);
                    camelContext.getRegistry().bind(getId(override.getName()), overrideParams);
                }
            }
        } else {
            registerDeprecatedSSLContextParameters();
        }
    }

    private void validateUniqueConfigurationNames() {
        List<String> allNames = getAllNames();

        List<String> duplicates = allNames.stream()
            .filter(name -> allNames.stream().filter(n -> n.equals(name)).count() > 1)
            .distinct()
            .toList();

        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException(
                "Duplicate TLS configuration names found: " + String.join(", ", duplicates)
            );
        }
    }

    private @NonNull List<String> getAllNames() {
        List<String> allNames = new ArrayList<>();
        if (tlsProperties.getDefaultConfig() != null && tlsProperties.getDefaultConfig().getName() != null) {
            allNames.add(tlsProperties.getDefaultConfig().getName());
        }
        if (tlsProperties.getOverrides() != null) {
            for (var override : tlsProperties.getOverrides()) {
                if (override.getName() != null) {
                    allNames.add(override.getName());
                }
            }
        }
        return allNames;
    }

    private void registerDeprecatedSSLContextParameters() {
        log.warn("Using deprecated SSL context configuration. Please migrate to vp.tls configuration.");
        SSLContextParameters params = getDeprecatedOutgoingSSLContextParameters(sslBundles, deprecatedBundle);
        camelContext.getRegistry().bind(getId(DEPRECATED_CONTEXT), params);
    }

    @Bean
    public SSLContextParameters incomingSSLContextParameters(SslBundles sslBundles, @Value("${tp.tls.store.consumer.bundle:}") String bundle) {
        SSLContextParameters sslContextParameters;
        if (bundle.isBlank()) { // no bundle, lets do it legacy style
            sslContextParameters = getSslContextParameters(securityProperies.getStore().getProducer());
        } else {
            sslContextParameters = getBundleBasedParameters(sslBundles, bundle);
        }

        SecureSocketProtocolsParameters sspp = createSecureProtocolParameters(securityProperies.getAllowedIncomingProtocols());
        sslContextParameters.setSecureSocketProtocols(sspp);
        sslContextParameters.setSecureSocketProtocol(sspp.getSecureSocketProtocol().get(0));

        // Set cipher suites
        if (dontUseAllCiphers(securityProperies.getAllowedIncomingCipherSuites())) {
            CipherSuitesParameters cipherSuites = createCipherSuiteParameters(securityProperies.getAllowedIncomingCipherSuites());
            sslContextParameters.setCipherSuites(cipherSuites);
        }
        return sslContextParameters;
    }

    private SSLContextParameters getSslContextParameters(SecurityProperties.Store.SSLConfig store) {
        SSLContextParameters sslContextParameters;
        sslContextParameters = new SSLContextParameters();
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(securityProperies.getStore().getLocation() + store.getFile());
        ksp.setPassword(store.getPassword());
        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword(store.getKeyPassword());
        kmp.setKeyStore(ksp);

        sslContextParameters.setKeyManagers(kmp);

        TrustManagersParameters trustManagersParameters = createTrustManagerParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);
        return sslContextParameters;
    }

    public static SSLContextParameters getBundleBasedParameters(SslBundles sslBundles, String bundle) {
        SslBundle sslBundle = sslBundles.getBundle(bundle);
        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(new KeyManagersParameters() {
            @Override
            public KeyManager[] createKeyManagers() {
                return sslBundle.getManagers().getKeyManagers();
            }
        });

        sslContextParameters.setTrustManagers(new TrustManagersParameters() {
            @Override
            public TrustManager[] createTrustManagers() {
                return sslBundle.getManagers().getTrustManagers();
            }
        });

        return sslContextParameters;
    }

    private boolean dontUseAllCiphers(String s) {
        return (s != null && !s.isBlank() && !s.trim().equals("*"));
    }

    private SecureSocketProtocolsParameters createSecureProtocolParameters(String allowedProtocolsString) {
        SecureSocketProtocolsParameters sspp = new SecureSocketProtocolsParameters();
        for (String protocol : allowedProtocolsString.split(DELIMITER)) {
            protocol = protocol.trim();
            if (!protocol.isEmpty()) {
                sspp.getSecureSocketProtocol().add(protocol);
            }
        }
        return sspp;
    }

    private CipherSuitesParameters createCipherSuiteParameters(String cipherSuiteString) {
        CipherSuitesParameters cipherSuites = new CipherSuitesParameters();
        List<String> allowedCipherSuites = new ArrayList<>();
        for (String protocol : cipherSuiteString.split(DELIMITER)) {
            protocol = protocol.trim();
            if (!protocol.isEmpty()) {
                allowedCipherSuites.add(protocol);
            }
        }
        cipherSuites.setCipherSuite(allowedCipherSuites);
        return cipherSuites;
    }

    private TrustManagersParameters createTrustManagerParameters() {
        KeyStoreParameters tsp = new KeyStoreParameters();
        tsp.setResource(securityProperies.getStore().getLocation() + securityProperies.getStore().getTruststore().getFile());
        tsp.setPassword(securityProperies.getStore().getTruststore().getPassword());
        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(tsp);
        return tmp;
    }

    private SSLContextParameters createSSLContextParameters(TLSProperties.TlSConfig config) throws GeneralSecurityException, IOException {
        SSLContextParameters params = getBundleBasedParameters(sslBundles, config.getBundle());

        List<String> protocols = determineProtocols(config, params);
        if (!protocols.isEmpty()) {
            SecureSocketProtocolsParameters sspp = new SecureSocketProtocolsParameters();
            sspp.getSecureSocketProtocol().addAll(protocols);
            params.setSecureSocketProtocols(sspp);
        }

        List<String> cipherSuites = determineCipherSuites(config, params);
        if (!cipherSuites.isEmpty()) {
            CipherSuitesParameters csp = new CipherSuitesParameters();
            csp.getCipherSuite().addAll(cipherSuites);
            params.setCipherSuites(csp);
        }

        return params;
    }

    private List<String> determineProtocols(TLSProperties.TlSConfig config, SSLContextParameters params) throws GeneralSecurityException, IOException {
        if (config.getProtocolsInclude() != null && !config.getProtocolsInclude().isEmpty()) {
            return config.getProtocolsInclude();
        }

        if (config.getProtocolsExclude() != null && !config.getProtocolsExclude().isEmpty()) {
            List<String> defaultProtocols = getDefaultProtocols(params);
            defaultProtocols.removeAll(config.getProtocolsExclude());
            return defaultProtocols;
        }

        return List.of();
    }

    private List<String> determineCipherSuites(TLSProperties.TlSConfig config, SSLContextParameters params) throws GeneralSecurityException, IOException {
        if (config.getCipherSuitesInclude() != null && !config.getCipherSuitesInclude().isEmpty()) {
            return config.getCipherSuitesInclude();
        }

        if (config.getCipherSuitesExclude() != null && !config.getCipherSuitesExclude().isEmpty()) {
            List<String> defaultCipherSuites = getDefaultCipherSuites(params);
            defaultCipherSuites.removeAll(config.getCipherSuitesExclude());
            return defaultCipherSuites;
        }

        return List.of();
    }

    private List<String> getDefaultProtocols(SSLContextParameters params) throws GeneralSecurityException, IOException {
        SSLContext sslContext = params.createSSLContext(camelContext);
        return new ArrayList<>(List.of(sslContext.getDefaultSSLParameters().getProtocols()));
    }

    private List<String> getDefaultCipherSuites(SSLContextParameters params) throws GeneralSecurityException, IOException {
        SSLContext sslContext = params.createSSLContext(camelContext);
        return new ArrayList<>(List.of(sslContext.getDefaultSSLParameters().getCipherSuites()));
    }

    private SSLContextParameters getDeprecatedOutgoingSSLContextParameters(SslBundles sslBundles, String bundle) {
        SSLContextParameters sslContextParameters;
        if (bundle.isBlank()) {
            sslContextParameters = getSslContextParameters(securityProperies.getStore().getConsumer());
        } else {
            sslContextParameters = getBundleBasedParameters(sslBundles, bundle);
        }
        SecureSocketProtocolsParameters sspp = createSecureProtocolParameters(securityProperies.getAllowedOutgoingProtocols());
        sslContextParameters.setSecureSocketProtocols(sspp);

        // Set cipher suites
        if (dontUseAllCiphers(securityProperies.getAllowedOutgoingCipherSuites())) {
            CipherSuitesParameters cipherSuites = createCipherSuiteParameters(securityProperies.getAllowedOutgoingCipherSuites());
            sslContextParameters.setCipherSuites(cipherSuites);
        }
        return sslContextParameters;
    }
}