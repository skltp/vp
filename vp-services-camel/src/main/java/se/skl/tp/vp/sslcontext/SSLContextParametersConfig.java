package se.skl.tp.vp.sslcontext;

import jakarta.annotation.PostConstruct;
import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.*;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.lang.NonNull;
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
import se.skl.tp.vp.logging.logentry.EcsTlsLogEntry;


import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Log4j2
@Configuration
public class SSLContextParametersConfig {

    public static final String DELIMITER = ",";
    public static final String DEPRECATED_CONTEXT = "default";

    private final SecurityProperties securityProperties;
    private final CamelContext camelContext;
    private final SslBundles sslBundles;
    private final TLSProperties tlsProperties;

    @Value("${tp.tls.store.consumer.bundle:}") String deprecatedBundle;

    public SSLContextParametersConfig(SecurityProperties securityProperties, CamelContext camelContext, SslBundles sslBundles, TLSProperties tlsProperties) {
        this.securityProperties = securityProperties;
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
            registerSSLContext(tlsProperties.getDefaultConfig());
            if (tlsProperties.getOverrides() != null) {
                for (var override : tlsProperties.getOverrides()) {
                    registerSSLContext(override);
                }
            }
        } else {
            registerDeprecatedSSLContext();
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

    private void registerSSLContext(TLSProperties.TLSConfig configuration) throws GeneralSecurityException, IOException {
        if (log.isDebugEnabled()) {
            logTLSConfiguration(configuration);
        }

        SSLContextParameters params = createSSLContextParameters(configuration);

        // Wrap with mTLS verification if enabled
        if (tlsProperties.isMtlsVerificationEnabled()) {
            log.info("Wrapping SSL context '{}' with mTLS verification",
                configuration.getName());
            params = new MtlsAwareSSLContextParameters(params,
                tlsProperties.isMtlsVerificationEnabled());
        }

        String id = getId(configuration.getName());
        if (log.isInfoEnabled()) {
            logResolvedConfiguration(id, params);
        }

        camelContext.getRegistry().bind(id, params);
    }

    private void registerDeprecatedSSLContext() {
        log.warn("Using deprecated SSL context configuration. Please migrate to vp.tls configuration.");
        SSLContextParameters params = getDeprecatedOutgoingSSLContextParameters(sslBundles, deprecatedBundle);

        // Wrap with mTLS verification if enabled
        if (tlsProperties.isMtlsVerificationEnabled()) {
            log.info("Wrapping deprecated SSL context with mTLS verification");
            params = new MtlsAwareSSLContextParameters(params,
                tlsProperties.isMtlsVerificationEnabled());
        }

        camelContext.getRegistry().bind(getId(DEPRECATED_CONTEXT), params);
    }

    @Bean
    public SSLContextParameters incomingSSLContextParameters(SslBundles sslBundles, @Value("${tp.tls.store.consumer.bundle:}") String bundle) {
        SSLContextParameters sslContextParameters;
        if (bundle.isBlank()) { // no bundle, lets do it legacy style
            sslContextParameters = getSslContextParameters(securityProperties.getStore().getProducer());
        } else {
            sslContextParameters = getBundleBasedParameters(sslBundles, bundle);
        }

        SecureSocketProtocolsParameters sspp = createSecureProtocolParameters(securityProperties.getAllowedIncomingProtocols());
        sslContextParameters.setSecureSocketProtocols(sspp);
        sslContextParameters.setSecureSocketProtocol(sspp.getSecureSocketProtocol().get(0));

        // Set cipher suites
        if (notAllowingAllCiphers(securityProperties.getAllowedIncomingCipherSuites())) {
            CipherSuitesParameters cipherSuites = createCipherSuiteParameters(securityProperties.getAllowedIncomingCipherSuites());
            sslContextParameters.setCipherSuites(cipherSuites);
        }
        return sslContextParameters;
    }

    private SSLContextParameters getSslContextParameters(SecurityProperties.Store.SSLConfig store) {
        SSLContextParameters sslContextParameters;
        sslContextParameters = new SSLContextParameters();
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(securityProperties.getStore().getLocation() + store.getFile());
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

        if (log.isDebugEnabled()) {
            log.debug("        Loading SSL bundle: {}", bundle);
            logKeyManagers(sslBundle.getManagers());
            logTrustManagers(sslBundle.getManagers());
        }

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

    private boolean notAllowingAllCiphers(String s) {
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
        tsp.setResource(securityProperties.getStore().getLocation() + securityProperties.getStore().getTruststore().getFile());
        tsp.setPassword(securityProperties.getStore().getTruststore().getPassword());
        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(tsp);
        return tmp;
    }

    private SSLContextParameters createSSLContextParameters(TLSProperties.TLSConfig config) throws GeneralSecurityException, IOException {
        SSLContextParameters params = getBundleBasedParameters(sslBundles, config.getBundle());

        List<String> protocols = determineProtocols(config, params);
        if (!protocols.isEmpty()) {
            log.debug("    Setting protocols: {}", protocols);
            SecureSocketProtocolsParameters sspp = new SecureSocketProtocolsParameters();
            sspp.getSecureSocketProtocol().addAll(protocols);
            params.setSecureSocketProtocols(sspp);
        } else {
            log.debug("    No protocols specified, using bundle defaults");
        }

        List<String> cipherSuites = determineCipherSuites(config, params);
        if (!cipherSuites.isEmpty()) {
            log.debug("    Setting cipher suites (count={}): {}", cipherSuites.size(), cipherSuites);
            CipherSuitesParameters csp = new CipherSuitesParameters();
            csp.getCipherSuite().addAll(cipherSuites);
            params.setCipherSuites(csp);
        } else {
            log.debug("    No cipher suites specified, using bundle defaults");
        }

        return params;
    }

    private List<String> determineProtocols(TLSProperties.TLSConfig config, SSLContextParameters params) throws GeneralSecurityException, IOException {
        if (config.getProtocolsInclude() != null && !config.getProtocolsInclude().isEmpty()) {
            log.debug("      Using protocol include list: {}", config.getProtocolsInclude());
            return config.getProtocolsInclude();
        }

        if (config.getProtocolsExclude() != null && !config.getProtocolsExclude().isEmpty()) {
            List<String> defaultProtocols = getDefaultProtocols(params);
            log.debug("      Using protocol exclude list. Defaults: {}, Excluding: {}",
                defaultProtocols, config.getProtocolsExclude());
            defaultProtocols.removeAll(config.getProtocolsExclude());
            log.debug("      Resulting protocols after exclusion: {}", defaultProtocols);
            return defaultProtocols;
        }

        log.debug("      No protocol filters specified");
        return List.of();
    }

    private List<String> determineCipherSuites(TLSProperties.TLSConfig config, SSLContextParameters params) throws GeneralSecurityException, IOException {
        if (config.getCipherSuitesInclude() != null && !config.getCipherSuitesInclude().isEmpty()) {
            log.debug("      Using cipher suite include list (count={}): {}",
                config.getCipherSuitesInclude().size(), config.getCipherSuitesInclude());
            return config.getCipherSuitesInclude();
        }

        if (config.getCipherSuitesExclude() != null && !config.getCipherSuitesExclude().isEmpty()) {
            List<String> defaultCipherSuites = getDefaultCipherSuites(params);
            log.debug("      Using cipher suite exclude list. Defaults count: {}, Excluding count: {}",
                defaultCipherSuites.size(), config.getCipherSuitesExclude().size());
            log.debug("      Excluding cipher suites: {}", config.getCipherSuitesExclude());
            defaultCipherSuites.removeAll(config.getCipherSuitesExclude());
            log.debug("      Resulting cipher suites after exclusion (count={}): {}",
                defaultCipherSuites.size(), defaultCipherSuites);
            return defaultCipherSuites;
        }

        log.debug("      No cipher suite filters specified");
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
            sslContextParameters = getSslContextParameters(securityProperties.getStore().getConsumer());
        } else {
            sslContextParameters = getBundleBasedParameters(sslBundles, bundle);
        }
        SecureSocketProtocolsParameters sspp = createSecureProtocolParameters(securityProperties.getAllowedOutgoingProtocols());
        sslContextParameters.setSecureSocketProtocols(sspp);

        // Set cipher suites
        if (notAllowingAllCiphers(securityProperties.getAllowedOutgoingCipherSuites())) {
            CipherSuitesParameters cipherSuites = createCipherSuiteParameters(securityProperties.getAllowedOutgoingCipherSuites());
            sslContextParameters.setCipherSuites(cipherSuites);
        }
        return sslContextParameters;
    }

    private static void logTLSConfiguration(TLSProperties.TLSConfig configuration) {
        log.debug("Creating SSL context '{}' with bundle: {}", configuration.getName(), configuration.getBundle());
        log.debug("  Configuration - protocolsInclude: {}, protocolsExclude: {}",
                configuration.getProtocolsInclude(), configuration.getProtocolsExclude());
        log.debug("  Configuration - cipherSuitesInclude: {}, cipherSuitesExclude: {}",
                configuration.getCipherSuitesInclude(), configuration.getCipherSuitesExclude());

        // Log override match criteria if this is an override
        if (configuration instanceof TLSProperties.TLSOverride override) {
            TLSProperties.TLSConfigMatch match = override.getMatch();
            if (match != null) {
                log.debug("  Override match criteria - domainName: {}, domainSuffix: {}, port: {}",
                        match.getDomainName(), match.getDomainSuffix(), match.getPort());
            }
        }
    }

    private static void logResolvedConfiguration(String id, SSLContextParameters params) {
        List<String> finalProtocols = params.getSecureSocketProtocols() != null ?
                params.getSecureSocketProtocols().getSecureSocketProtocol() : List.of();
        List<String> finalCipherSuites = params.getCipherSuites() != null ?
                params.getCipherSuites().getCipherSuite() : List.of();

        // Determine if using explicitly configured values or bundle defaults
        String protocolsSource = params.getSecureSocketProtocols() != null ? "configured" : "bundle defaults";
        String cipherSuitesSource = params.getCipherSuites() != null ? "configured" : "bundle defaults";

        EcsTlsLogEntry logEntry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .withSslContextId(id)
                .withProtocolsAndCipherSuites(finalProtocols, finalCipherSuites)
                .withMessage(String.format("Registering SSL Context with id '%s' - protocols (%d, %s): %s, cipher suites (%d, %s)",
                        id, finalProtocols.size(), protocolsSource,
                        finalProtocols.isEmpty() ? "using bundle defaults" : finalProtocols,
                        finalCipherSuites.size(), cipherSuitesSource))
                .build();
        log.info(logEntry);
    }


    private static void logTrustManagers(SslManagerBundle managers) {
        TrustManager[] trustManagers = managers.getTrustManagers();
        if (trustManagers == null || trustManagers.length == 0) {
            log.debug("        TrustManagers: none (will not validate server certificates)");
            return;
        }

        log.debug("        TrustManagers loaded: {} manager(s)", trustManagers.length);
        for (int i = 0; i < trustManagers.length; i++) {
            TrustManager tm = trustManagers[i];
            log.debug("          [{}] Type: {}", i, tm.getClass().getName());
            if (tm instanceof X509TrustManager x509tm) {
                logX509TrustManager(i, x509tm);
            }
        }
    }

    private static void logX509TrustManager(int index, X509TrustManager x509tm) {
        X509Certificate[] acceptedIssuers = x509tm.getAcceptedIssuers();
        int count = acceptedIssuers != null ? acceptedIssuers.length : 0;
        log.debug("          [{}] Accepted issuers: {} CA(s)", index, count);

        if (acceptedIssuers != null && acceptedIssuers.length > 0) {
            for (int i = 0; i < acceptedIssuers.length; i++) {
                X509Certificate cert = acceptedIssuers[i];
                log.debug("            CA[{}] Subject: {}", i, cert.getSubjectX500Principal().getName());
                log.debug("            CA[{}] Issuer:  {}", i, cert.getIssuerX500Principal().getName());
                log.debug("            CA[{}] Valid from {} to {}", i, cert.getNotBefore(), cert.getNotAfter());
                log.debug("            CA[{}] Serial: {}", i, cert.getSerialNumber());

                // Log SHA-256 fingerprint for certificate identification
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] fingerprint = digest.digest(cert.getEncoded());
                    String fingerprintHex = HexFormat.of().withUpperCase().withDelimiter(":").formatHex(fingerprint);
                    log.debug("            CA[{}] SHA-256: {}", i, fingerprintHex);
                } catch (Exception e) {
                    log.debug("            CA[{}] Unable to compute fingerprint: {}", i, e.getMessage());
                }
            }
        }
    }

    private static void logKeyManagers(SslManagerBundle managers) {
        KeyManager[] keyManagers = managers.getKeyManagers();
        if (keyManagers == null || keyManagers.length == 0) {
            log.debug("        KeyManagers: none (client certificates will NOT be sent)");
            return;
        }

        log.debug("        KeyManagers loaded: {} manager(s)", keyManagers.length);
        for (int i = 0; i < keyManagers.length; i++) {
            KeyManager km = keyManagers[i];
            log.debug("          [{}] Type: {}", i, km.getClass().getName());
            if (km instanceof X509KeyManager x509km) {
                logX509KeyManager(i, x509km);
            }
        }
    }

    private static void logX509KeyManager(int index, X509KeyManager x509km) {
        String[] aliases = x509km.getClientAliases("RSA", null);
        String aliasesDisplay = aliases != null ? String.join(", ", aliases) : "none";
        log.debug("          [{}] Client aliases (RSA): {}", index, aliasesDisplay);

        if (aliases != null && aliases.length > 0) {
            int chainLength = x509km.getCertificateChain(aliases[0]) != null ?
                    x509km.getCertificateChain(aliases[0]).length : 0;
            log.debug("          [{}] Client certificate chain for '{}': {} cert(s)",
                    index, aliases[0], chainLength);
        }
    }
}

