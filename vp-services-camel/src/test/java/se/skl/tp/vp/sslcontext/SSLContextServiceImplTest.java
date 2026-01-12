package se.skl.tp.vp.sslcontext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.skl.tp.vp.config.TLSProperties;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SSLContextServiceImplTest {

    private TLSProperties tlsProperties;
    private SSLContextServiceImpl sslContextService;

    @BeforeEach
    void setUp() {
        tlsProperties = new TLSProperties();
        sslContextService = new SSLContextServiceImpl(tlsProperties);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDefaultConfigTestCases")
    void testGetClientSSLContextId_WithDefaultConfig(String testName, String vagvalHost, String expectedContextId) {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);

        String contextId = sslContextService.getClientSSLContextId(vagvalHost);

        assertEquals(expectedContextId, contextId);
    }

    private static Stream<Arguments> provideDefaultConfigTestCases() {
        return Stream.of(
            Arguments.of("with port", "example.com:443", "SSLContext-default"),
            Arguments.of("without port", "example.com", "SSLContext-default")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDomainMatchTestCases")
    void testGetClientSSLContextId_WithDomainMatch(String testName, String matchDomain, String vagvalHost, String expectedContextId) {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override = new TLSProperties.TLSOverride();
        override.setName("special");
        TLSProperties.TLSConfigMatch match = new TLSProperties.TLSConfigMatch();
        match.setDomainName(matchDomain);
        override.setMatch(match);
        tlsProperties.setOverrides(List.of(override));

        String contextId = sslContextService.getClientSSLContextId(vagvalHost);

        assertEquals(expectedContextId, contextId);
    }

    private static Stream<Arguments> provideDomainMatchTestCases() {
        return Stream.of(
            Arguments.of("exact match", "special.example.com", "special.example.com:443", "SSLContext-special"),
            Arguments.of("case insensitive", "SPECIAL.EXAMPLE.COM", "special.example.com:443", "SSLContext-special")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDomainSuffixMatchTestCases")
    void testGetClientSSLContextId_WithDomainSuffixMatch(String testName, String matchSuffix, String vagvalHost, String expectedContextId) {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override = new TLSProperties.TLSOverride();
        override.setName("internal");
        TLSProperties.TLSConfigMatch match = new TLSProperties.TLSConfigMatch();
        match.setDomainSuffix(matchSuffix);
        override.setMatch(match);
        tlsProperties.setOverrides(List.of(override));

        String contextId = sslContextService.getClientSSLContextId(vagvalHost);

        assertEquals(expectedContextId, contextId);
    }

    private static Stream<Arguments> provideDomainSuffixMatchTestCases() {
        return Stream.of(
            Arguments.of("exact suffix match", ".internal.com", "api.internal.com:443", "SSLContext-internal"),
            Arguments.of("case insensitive suffix", ".INTERNAL.COM", "API.internal.com:443", "SSLContext-internal")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("providePortMatchTestCases")
    void testGetClientSSLContextId_WithPortMatch(String testName, Integer matchPort, String vagvalHost, String expectedContextId) {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override = new TLSProperties.TLSOverride();
        override.setName("port8443");
        TLSProperties.TLSConfigMatch match = new TLSProperties.TLSConfigMatch();
        match.setDomainSuffix(".example.com");
        match.setPort(matchPort);
        override.setMatch(match);
        tlsProperties.setOverrides(List.of(override));

        String contextId = sslContextService.getClientSSLContextId(vagvalHost);

        assertEquals(expectedContextId, contextId);
    }

    private static Stream<Arguments> providePortMatchTestCases() {
        return Stream.of(
            Arguments.of("port matches", 8443, "api.example.com:8443", "SSLContext-port8443"),
            Arguments.of("port mismatch uses default", 8443, "api.example.com:443", "SSLContext-default")
        );
    }

    @Test
    void testGetClientSSLContextId_WithMultipleOverrides_FirstMatchWins() {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override1 = new TLSProperties.TLSOverride();
        override1.setName("first");
        TLSProperties.TLSConfigMatch match1 = new TLSProperties.TLSConfigMatch();
        match1.setDomainSuffix(".example.com");
        override1.setMatch(match1);
        TLSProperties.TLSOverride override2 = new TLSProperties.TLSOverride();
        override2.setName("second");
        TLSProperties.TLSConfigMatch match2 = new TLSProperties.TLSConfigMatch();
        match2.setDomainName("api.example.com");
        override2.setMatch(match2);
        tlsProperties.setOverrides(Arrays.asList(override1, override2));

        String contextId = sslContextService.getClientSSLContextId("api.example.com:443");

        assertEquals("SSLContext-first", contextId, "First matching override should be used");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideOverrideListTestCases")
    void testGetClientSSLContextId_WithEmptyOverrideList(String testName, List<TLSProperties.TLSOverride> overrides, String expectedContextId) {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        tlsProperties.setOverrides(overrides);

        String contextId = sslContextService.getClientSSLContextId("example.com:443");

        assertEquals(expectedContextId, contextId);
    }

    private static Stream<Arguments> provideOverrideListTestCases() {
        return Stream.of(
            Arguments.of("null overrides", null, "SSLContext-default"),
            Arguments.of("empty overrides", List.of(), "SSLContext-default")
        );
    }

    @Test
    void testGetClientSSLContextId_WithOverride_NoMatch() {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override = new TLSProperties.TLSOverride();
        override.setName("special");
        TLSProperties.TLSConfigMatch match = new TLSProperties.TLSConfigMatch();
        match.setDomainName("special.example.com");
        override.setMatch(match);
        tlsProperties.setOverrides(List.of(override));

        String contextId = sslContextService.getClientSSLContextId("other.example.com:443");

        assertEquals("SSLContext-default", contextId);
    }

    @Test
    void testGetClientSSLContextId_WithOverride_NullMatch_IsIgnored() {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override1 = new TLSProperties.TLSOverride();
        override1.setName("invalid");
        override1.setMatch(null); // Invalid - should be ignored
        TLSProperties.TLSOverride override2 = new TLSProperties.TLSOverride();
        override2.setName("valid");
        TLSProperties.TLSConfigMatch match2 = new TLSProperties.TLSConfigMatch();
        match2.setDomainName("example.com");
        override2.setMatch(match2);
        tlsProperties.setOverrides(Arrays.asList(override1, override2));

        String contextId = sslContextService.getClientSSLContextId("example.com:443");

        assertEquals("SSLContext-valid", contextId, "Override with null match should be ignored");
    }

    @Test
    void testGetClientSSLContextId_WithOverride_NullName_IsIgnored() {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override1 = new TLSProperties.TLSOverride();
        override1.setName(null); // Invalid - should be ignored
        TLSProperties.TLSConfigMatch match1 = new TLSProperties.TLSConfigMatch();
        match1.setDomainName("example.com");
        override1.setMatch(match1);
        TLSProperties.TLSOverride override2 = new TLSProperties.TLSOverride();
        override2.setName("valid");
        TLSProperties.TLSConfigMatch match2 = new TLSProperties.TLSConfigMatch();
        match2.setDomainName("example.com");
        override2.setMatch(match2);
        tlsProperties.setOverrides(Arrays.asList(override1, override2));

        String contextId = sslContextService.getClientSSLContextId("example.com:443");

        assertEquals("SSLContext-valid", contextId, "Override with null name should be ignored");
    }

    @Test
    void testGetClientSSLContextId_NoDefaultConfig_UsesDeprecated() {
        tlsProperties.setDefaultConfig(null);

        String contextId = sslContextService.getClientSSLContextId("example.com:443");

        assertEquals("SSLContext-default", contextId);
    }

    @Test
    void testGetClientSSLContextId_DefaultConfigWithNullName_ThrowsException() {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName(null);
        tlsProperties.setDefaultConfig(defaultConfig);

        assertThrows(IllegalStateException.class,
                () -> sslContextService.getClientSSLContextId("example.com:443"),
                "Should throw IllegalStateException when default config has null name");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCachingTestCases")
    void testGetClientSSLContextId_Caching(String testName, String host1, String host2, String expectedId1, String expectedId2, boolean shouldBeSame) {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override = new TLSProperties.TLSOverride();
        override.setName("special");
        TLSProperties.TLSConfigMatch match = new TLSProperties.TLSConfigMatch();
        match.setDomainName("special.example.com");
        override.setMatch(match);
        tlsProperties.setOverrides(List.of(override));

        String contextId1 = sslContextService.getClientSSLContextId(host1);
        String contextId2 = sslContextService.getClientSSLContextId(host2);

        assertEquals(expectedId1, contextId1);
        assertEquals(expectedId2, contextId2);
        if (shouldBeSame) {
            assertSame(contextId1, contextId2, "Same host should return cached result");
        } else {
            assertNotSame(contextId1, contextId2, "Different hosts should return different results");
        }
    }

    private static Stream<Arguments> provideCachingTestCases() {
        return Stream.of(
            Arguments.of("same host returns same result", "example.com:443", "example.com:443",
                "SSLContext-default", "SSLContext-default", true),
            Arguments.of("different hosts return different results", "example.com:443", "special.example.com:443",
                "SSLContext-default", "SSLContext-special", false)
        );
    }

    @Test
    void testGetClientSSLContextId_NoMatchCriteria_ReturnsFalse() {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override = new TLSProperties.TLSOverride();
        override.setName("noop");
        TLSProperties.TLSConfigMatch match = new TLSProperties.TLSConfigMatch();
        override.setMatch(match);
        tlsProperties.setOverrides(List.of(override));

        String contextId = sslContextService.getClientSSLContextId("example.com:443");

        assertEquals("SSLContext-default", contextId, "Should use default when override has no match criteria");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("providePortCombinationTestCases")
    void testGetClientSSLContextId_PortCombinations(String testName, String vagvalHost, String expectedContextId) {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override = new TLSProperties.TLSOverride();
        override.setName("port8443");
        TLSProperties.TLSConfigMatch match = new TLSProperties.TLSConfigMatch();
        match.setDomainSuffix(".example.com");
        match.setPort(8443);
        override.setMatch(match);
        tlsProperties.setOverrides(List.of(override));

        String contextId = sslContextService.getClientSSLContextId(vagvalHost);

        assertEquals(expectedContextId, contextId);
    }

    private static Stream<Arguments> providePortCombinationTestCases() {
        return Stream.of(
            Arguments.of("domain and port both match", "api.example.com:8443", "SSLContext-port8443"),
            Arguments.of("domain matches but port doesn't", "api.example.com:443", "SSLContext-default")
        );
    }

    @Test
    void testGetClientSSLContextId_DefaultPortAssumption() {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override = new TLSProperties.TLSOverride();
        override.setName("port443");
        TLSProperties.TLSConfigMatch match = new TLSProperties.TLSConfigMatch();
        match.setDomainName("example.com");
        match.setPort(443);
        override.setMatch(match);
        tlsProperties.setOverrides(List.of(override));

        String contextId = sslContextService.getClientSSLContextId("example.com");

        assertEquals("SSLContext-port443", contextId);
    }

    @Test
    void testGetClientSSLContextId_WithDomainNameAndSuffixBothSet_DomainNameTakesPrecedence() {
        TLSProperties.TlSConfig defaultConfig = new TLSProperties.TlSConfig();
        defaultConfig.setName("default");
        tlsProperties.setDefaultConfig(defaultConfig);
        TLSProperties.TLSOverride override = new TLSProperties.TLSOverride();
        override.setName("special");
        TLSProperties.TLSConfigMatch match = new TLSProperties.TLSConfigMatch();
        match.setDomainName("exact.example.com");
        match.setDomainSuffix(".example.com"); // This should be ignored when domainName is set
        override.setMatch(match);
        tlsProperties.setOverrides(List.of(override));

        String contextIdExact = sslContextService.getClientSSLContextId("exact.example.com:443");
        String contextIdOther = sslContextService.getClientSSLContextId("other.example.com:443");

        assertEquals("SSLContext-special", contextIdExact, "Should match exact domain name");
        assertEquals("SSLContext-default", contextIdOther, "Should not match suffix when domainName is set");
    }
}
