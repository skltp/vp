package se.skl.tp.vp.sslcontext;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import se.skl.tp.vp.config.SecurityProperties;
import se.skl.tp.vp.config.TLSProperties;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SSLContextParametersConfigTest {

    private CamelContext camelContext;
    private Registry registry;
    private SslBundles sslBundles;
    private TLSProperties tlsProperties;
    private SecurityProperties securityProperties;
    private SSLContextParametersConfig config;

    @BeforeEach
    void setUp() {
        camelContext = spy(new DefaultCamelContext());
        registry = camelContext.getRegistry();
        sslBundles = mock(SslBundles.class);
        tlsProperties = new TLSProperties();
        securityProperties = new SecurityProperties();

        // Setup mock SSL bundle
        setupMockSslBundle();
    }

    private void setupMockSslBundle() {
        SslBundle sslBundle = mock(SslBundle.class);
        SslManagerBundle managerBundle = mock(SslManagerBundle.class);

        // Create mock key managers
        KeyManager[] keyManagers = new KeyManager[]{createMockKeyManager()};
        TrustManager[] trustManagers = new TrustManager[]{createMockTrustManager()};

        when(sslBundle.getManagers()).thenReturn(managerBundle);
        when(managerBundle.getKeyManagers()).thenReturn(keyManagers);
        when(managerBundle.getTrustManagers()).thenReturn(trustManagers);

        when(sslBundles.getBundle(anyString())).thenReturn(sslBundle);
    }

    private X509KeyManager createMockKeyManager() {
        return mock(X509KeyManager.class);
    }

    private X509TrustManager createMockTrustManager() {
        X509TrustManager trustManager = mock(X509TrustManager.class);
        when(trustManager.getAcceptedIssuers()).thenReturn(new X509Certificate[0]);
        return trustManager;
    }

    @Test
    void testRegisterSSLContextParameters_WithDefaultConfigOnly() throws GeneralSecurityException, IOException {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("default");
        defaultConfig.setBundle("default-bundle");
        tlsProperties.setDefaultConfig(defaultConfig);

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);
        config.registerSSLContextParameters();

        String expectedId = SSLContextParametersConfig.getId("default");
        Object registered = registry.lookupByName(expectedId);
        assertNotNull(registered, "SSL context should be registered");
        assertInstanceOf(SSLContextParameters.class, registered);
        verify(sslBundles, times(1)).getBundle("default-bundle");
    }

    @Test
    void testRegisterSSLContextParameters_WithDefaultAndOverrides() throws GeneralSecurityException, IOException {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("default");
        defaultConfig.setBundle("default-bundle");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override1 = new TLSProperties.TLSOverride();
        override1.setName("override1");
        override1.setBundle("override1-bundle");
        TLSProperties.TLSOverride override2 = new TLSProperties.TLSOverride();
        override2.setName("override2");
        override2.setBundle("override2-bundle");
        tlsProperties.setOverrides(Arrays.asList(override1, override2));

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);
        config.registerSSLContextParameters();

        assertNotNull(registry.lookupByName(SSLContextParametersConfig.getId("default")));
        assertNotNull(registry.lookupByName(SSLContextParametersConfig.getId("override1")));
        assertNotNull(registry.lookupByName(SSLContextParametersConfig.getId("override2")));
        verify(sslBundles, times(1)).getBundle("default-bundle");
        verify(sslBundles, times(1)).getBundle("override1-bundle");
        verify(sslBundles, times(1)).getBundle("override2-bundle");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideProtocolTestCases")
    void testRegisterSSLContextParameters_WithProtocols(String testName, List<String> protocolsInclude,
                                                        List<String> protocolsExclude,
                                                        List<String> expectedIncluded,
                                                        List<String> expectedExcluded) throws GeneralSecurityException, IOException {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("default");
        defaultConfig.setBundle("default-bundle");
        defaultConfig.setProtocolsInclude(protocolsInclude);
        defaultConfig.setProtocolsExclude(protocolsExclude);
        tlsProperties.setDefaultConfig(defaultConfig);

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);
        config.registerSSLContextParameters();

        SSLContextParameters params = (SSLContextParameters) registry.lookupByName(SSLContextParametersConfig.getId("default"));
        assertNotNull(params);
        assertNotNull(params.getSecureSocketProtocols());
        List<String> protocols = params.getSecureSocketProtocols().getSecureSocketProtocol();
        if (expectedIncluded != null) {
            assertEquals(expectedIncluded.size(), protocols.size());
            expectedIncluded.forEach(protocol -> assertTrue(protocols.contains(protocol)));
        }
        if (expectedExcluded != null) {
            expectedExcluded.forEach(protocol -> assertFalse(protocols.contains(protocol)));
        }
    }

    private static Stream<Arguments> provideProtocolTestCases() {
        return Stream.of(
            Arguments.of("protocols include",
                Arrays.asList("TLSv1.3", "TLSv1.2"), null,
                Arrays.asList("TLSv1.3", "TLSv1.2"), null),
            Arguments.of("protocols exclude",
                null, Arrays.asList("TLSv1", "TLSv1.1"),
                null, Arrays.asList("TLSv1", "TLSv1.1"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCipherSuiteTestCases")
    void testRegisterSSLContextParameters_WithCipherSuites(String testName, List<String> cipherSuitesInclude,
                                                           List<String> cipherSuitesExclude,
                                                           List<String> expectedIncluded,
                                                           List<String> expectedExcluded) throws GeneralSecurityException, IOException {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("default");
        defaultConfig.setBundle("default-bundle");
        defaultConfig.setCipherSuitesInclude(cipherSuitesInclude);
        defaultConfig.setCipherSuitesExclude(cipherSuitesExclude);
        tlsProperties.setDefaultConfig(defaultConfig);

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);
        config.registerSSLContextParameters();

        SSLContextParameters params = (SSLContextParameters) registry.lookupByName(SSLContextParametersConfig.getId("default"));
        assertNotNull(params);
        assertNotNull(params.getCipherSuites());
        List<String> cipherSuites = params.getCipherSuites().getCipherSuite();
        if (expectedIncluded != null) {
            assertEquals(expectedIncluded.size(), cipherSuites.size());
            expectedIncluded.forEach(suite -> assertTrue(cipherSuites.contains(suite)));
        }
        if (expectedExcluded != null) {
            expectedExcluded.forEach(suite -> assertFalse(cipherSuites.contains(suite)));
        }
    }

    private static Stream<Arguments> provideCipherSuiteTestCases() {
        return Stream.of(
            Arguments.of("cipher suites include",
                Arrays.asList("TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256"), null,
                Arrays.asList("TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256"), null),
            Arguments.of("cipher suites exclude",
                null, List.of("TLS_RSA_WITH_AES_128_CBC_SHA"),
                null, List.of("TLS_RSA_WITH_AES_128_CBC_SHA"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideOverrideTestCases")
    void testRegisterSSLContextParameters_WithOverrides(String testName, List<TLSProperties.TLSOverride> overrides) throws GeneralSecurityException, IOException {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("default");
        defaultConfig.setBundle("default-bundle");
        tlsProperties.setDefaultConfig(defaultConfig);
        tlsProperties.setOverrides(overrides);

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);
        config.registerSSLContextParameters();

        assertNotNull(registry.lookupByName(SSLContextParametersConfig.getId("default")));
        verify(sslBundles, times(1)).getBundle("default-bundle");
    }

    private static Stream<Arguments> provideOverrideTestCases() {
        return Stream.of(
            Arguments.of("null overrides", null),
            Arguments.of("empty overrides", List.of())
        );
    }

    @Test
    void testRegisterSSLContextParameters_DeprecatedMode_WithEmptyBundle() throws GeneralSecurityException, IOException {
        tlsProperties.setDefaultConfig(null);
        SecurityProperties.Store store = new SecurityProperties.Store();
        SecurityProperties.Store.SSLConfig consumer = new SecurityProperties.Store.SSLConfig();
        consumer.setFile("consumer.jks");
        consumer.setPassword("password");
        consumer.setKeyPassword("keypass");
        SecurityProperties.Store.Truststore truststore = new SecurityProperties.Store.Truststore();
        truststore.setFile("truststore.jks");
        truststore.setPassword("trustpass");
        store.setLocation("file:/path/to/");
        store.setConsumer(consumer);
        store.setTruststore(truststore);
        securityProperties.setStore(store);
        securityProperties.setAllowedOutgoingProtocols("TLSv1.3,TLSv1.2");
        securityProperties.setAllowedOutgoingCipherSuites("*");

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);
        config.deprecatedBundle = "";
        config.registerSSLContextParameters();

        String expectedId = SSLContextParametersConfig.getId(SSLContextParametersConfig.DEPRECATED_CONTEXT);
        SSLContextParameters params = (SSLContextParameters) registry.lookupByName(expectedId);
        assertNotNull(params, "Deprecated SSL context should be registered");
        assertNotNull(params.getKeyManagers(), "Key managers should be configured");
        assertNotNull(params.getTrustManagers(), "Trust managers should be configured");
        assertNotNull(params.getSecureSocketProtocols());
        assertEquals(2, params.getSecureSocketProtocols().getSecureSocketProtocol().size());
    }

    @Test
    void testRegisterSSLContextParameters_DeprecatedMode_WithBundle() throws GeneralSecurityException, IOException {
        tlsProperties.setDefaultConfig(null);
        securityProperties.setAllowedOutgoingProtocols("TLSv1.3,TLSv1.2");
        securityProperties.setAllowedOutgoingCipherSuites("TLS_AES_256_GCM_SHA384,TLS_AES_128_GCM_SHA256");

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);
        config.deprecatedBundle = "deprecated-bundle";
        config.registerSSLContextParameters();

        String expectedId = SSLContextParametersConfig.getId(SSLContextParametersConfig.DEPRECATED_CONTEXT);
        SSLContextParameters params = (SSLContextParameters) registry.lookupByName(expectedId);
        assertNotNull(params);
        assertNotNull(params.getSecureSocketProtocols());
        assertEquals(2, params.getSecureSocketProtocols().getSecureSocketProtocol().size());
        assertNotNull(params.getCipherSuites());
        assertEquals(2, params.getCipherSuites().getCipherSuite().size());
        verify(sslBundles, times(1)).getBundle("deprecated-bundle");
    }

    @Test
    void testRegisterSSLContextParameters_MultipleOverridesWithDifferentConfigurations() throws GeneralSecurityException, IOException {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("default");
        defaultConfig.setBundle("default-bundle");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override1 = new TLSProperties.TLSOverride();
        override1.setName("strict");
        override1.setBundle("strict-bundle");
        override1.setProtocolsInclude(List.of("TLSv1.3"));
        override1.setCipherSuitesInclude(List.of("TLS_AES_256_GCM_SHA384"));
        TLSProperties.TLSOverride override2 = new TLSProperties.TLSOverride();
        override2.setName("legacy");
        override2.setBundle("legacy-bundle");
        override2.setProtocolsInclude(Arrays.asList("TLSv1.2", "TLSv1.1"));
        tlsProperties.setOverrides(Arrays.asList(override1, override2));

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);
        config.registerSSLContextParameters();

        SSLContextParameters strictParams = (SSLContextParameters) registry.lookupByName(SSLContextParametersConfig.getId("strict"));
        assertNotNull(strictParams);
        assertEquals(1, strictParams.getSecureSocketProtocols().getSecureSocketProtocol().size());
        assertEquals("TLSv1.3", strictParams.getSecureSocketProtocols().getSecureSocketProtocol().get(0));
        assertEquals(1, strictParams.getCipherSuites().getCipherSuite().size());
        SSLContextParameters legacyParams = (SSLContextParameters) registry.lookupByName(SSLContextParametersConfig.getId("legacy"));
        SSLContext legacySslContext = legacyParams.createSSLContext(camelContext);
        Set<String> expectedCipherSuites = Set.of(legacySslContext.getDefaultSSLParameters().getCipherSuites());

        assertNotNull(legacyParams);
        assertEquals(2, legacyParams.getSecureSocketProtocols().getSecureSocketProtocol().size());
        assertEquals(expectedCipherSuites, new HashSet<>(legacyParams.getCipherSuites().getCipherSuite()));
    }

    @Test
    void testRegisterSSLContextParameters_DuplicateNameInOverrides_ThrowsException() {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("default");
        defaultConfig.setBundle("default-bundle");
        tlsProperties.setDefaultConfig(defaultConfig);

        TLSProperties.TLSOverride override1 = new TLSProperties.TLSOverride();
        override1.setName("duplicate");
        override1.setBundle("bundle1");

        TLSProperties.TLSOverride override2 = new TLSProperties.TLSOverride();
        override2.setName("duplicate");
        override2.setBundle("bundle2");

        tlsProperties.setOverrides(Arrays.asList(override1, override2));

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.registerSSLContextParameters());

        assertTrue(exception.getMessage().contains("Duplicate TLS configuration names found"));
        assertTrue(exception.getMessage().contains("duplicate"));
    }

    @Test
    void testRegisterSSLContextParameters_DefaultNameDuplicatedInOverride_ThrowsException() {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("sameName");
        defaultConfig.setBundle("default-bundle");
        tlsProperties.setDefaultConfig(defaultConfig);

        TLSProperties.TLSOverride override1 = new TLSProperties.TLSOverride();
        override1.setName("sameName");
        override1.setBundle("override-bundle");

        tlsProperties.setOverrides(List.of(override1));

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.registerSSLContextParameters());

        assertTrue(exception.getMessage().contains("Duplicate TLS configuration names found"));
        assertTrue(exception.getMessage().contains("sameName"));
    }

    @Test
    void testRegisterSSLContextParameters_MultipleDuplicates_ThrowsException() {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("default");
        defaultConfig.setBundle("default-bundle");
        tlsProperties.setDefaultConfig(defaultConfig);

        TLSProperties.TLSOverride override1 = new TLSProperties.TLSOverride();
        override1.setName("dup1");
        override1.setBundle("bundle1");

        TLSProperties.TLSOverride override2 = new TLSProperties.TLSOverride();
        override2.setName("dup1");
        override2.setBundle("bundle2");

        TLSProperties.TLSOverride override3 = new TLSProperties.TLSOverride();
        override3.setName("dup2");
        override3.setBundle("bundle3");

        TLSProperties.TLSOverride override4 = new TLSProperties.TLSOverride();
        override4.setName("dup2");
        override4.setBundle("bundle4");

        tlsProperties.setOverrides(Arrays.asList(override1, override2, override3, override4));

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.registerSSLContextParameters());

        assertTrue(exception.getMessage().contains("Duplicate TLS configuration names found"));
        assertTrue(exception.getMessage().contains("dup1"));
        assertTrue(exception.getMessage().contains("dup2"));
    }

    @Test
    void testRegisterSSLContextParameters_UniqueNames_NoException() {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("default");
        defaultConfig.setBundle("default-bundle");
        tlsProperties.setDefaultConfig(defaultConfig);

        TLSProperties.TLSOverride override1 = new TLSProperties.TLSOverride();
        override1.setName("override1");
        override1.setBundle("bundle1");

        TLSProperties.TLSOverride override2 = new TLSProperties.TLSOverride();
        override2.setName("override2");
        override2.setBundle("bundle2");

        TLSProperties.TLSOverride override3 = new TLSProperties.TLSOverride();
        override3.setName("override3");
        override3.setBundle("bundle3");

        tlsProperties.setOverrides(Arrays.asList(override1, override2, override3));

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);

        assertDoesNotThrow(() -> config.registerSSLContextParameters());

        assertNotNull(registry.lookupByName(SSLContextParametersConfig.getId("default")));
        assertNotNull(registry.lookupByName(SSLContextParametersConfig.getId("override1")));
        assertNotNull(registry.lookupByName(SSLContextParametersConfig.getId("override2")));
        assertNotNull(registry.lookupByName(SSLContextParametersConfig.getId("override3")));
    }

    @Test
    void testRegisterSSLContextParameters_NullNames_NoException() {
        TLSProperties.TLSConfig defaultConfig = new TLSProperties.TLSConfig();
        defaultConfig.setName("default");
        defaultConfig.setBundle("default-bundle");
        tlsProperties.setDefaultConfig(defaultConfig);

        TLSProperties.TLSOverride override1 = new TLSProperties.TLSOverride();
        override1.setName(null);
        override1.setBundle("bundle1");

        tlsProperties.setOverrides(List.of(override1));

        config = new SSLContextParametersConfig(securityProperties, camelContext, sslBundles, tlsProperties);

        // Should not throw exception for null names, but might fail during registration
        // This test verifies that null names don't cause NPE in validation
        assertDoesNotThrow(() -> config.registerSSLContextParameters());
    }
}


