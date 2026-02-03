package se.skl.tp.vp.sslcontext;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jsse.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import static org.junit.jupiter.api.Assertions.*;

class MtlsAwareSSLContextParametersTest {

    private CamelContext camelContext;
    private SSLContextParameters baseParams;

    @BeforeEach
    void setUp() {
        camelContext = new DefaultCamelContext();
        baseParams = createTestSSLContextParameters();
    }

    private SSLContextParameters createTestSSLContextParameters() {
        SSLContextParameters params = new SSLContextParameters();

        KeyStoreParameters keyStoreParams = new KeyStoreParameters();
        keyStoreParams.setResource("classpath:certs/tp.jks");
        keyStoreParams.setPassword("password");

        KeyManagersParameters keyManagers = new KeyManagersParameters();
        keyManagers.setKeyStore(keyStoreParams);
        keyManagers.setKeyPassword("password");
        params.setKeyManagers(keyManagers);

        TrustManagersParameters trustManagers = new TrustManagersParameters();
        KeyStoreParameters trustStoreParams = new KeyStoreParameters();
        trustStoreParams.setResource("classpath:certs/truststore.jks");
        trustStoreParams.setPassword("password");
        trustManagers.setKeyStore(trustStoreParams);
        params.setTrustManagers(trustManagers);

        return params;
    }

    @Test
    void testMtlsAwareSSLContextParametersCreation() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        assertNotNull(mtlsParams);
    }

    @Test
    void testCreateSSLContext() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);

        assertNotNull(sslContext);
    }

    @Test
    void testCreateSSLEngineWithMtlsVerificationEnabled() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();

        assertNotNull(engine);
        // Engine should be wrapped
        assertNotEquals(baseParams.createSSLContext(camelContext).createSSLEngine().getClass(),
                       engine.getClass());
    }

    @Test
    void testCreateSSLEngineWithMtlsVerificationDisabled() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, false);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();

        assertNotNull(engine);
    }

    @Test
    void testCreateSSLEngineWithHostAndPort() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine("localhost", 443);

        assertNotNull(engine);
        assertEquals("localhost", engine.getPeerHost());
        assertEquals(443, engine.getPeerPort());
    }

    @Test
    void testEngineMethodsDelegation() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();

        assertNotNull(engine.getSupportedCipherSuites());
        assertNotNull(engine.getSupportedProtocols());

        engine.setUseClientMode(true);
        assertTrue(engine.getUseClientMode());

        engine.setEnableSessionCreation(true);
        assertTrue(engine.getEnableSessionCreation());
    }

    @Test
    void testGetSetKeyManagers() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        KeyManagersParameters keyManagers = mtlsParams.getKeyManagers();
        assertNotNull(keyManagers);
        assertEquals(baseParams.getKeyManagers(), keyManagers);

        KeyManagersParameters newKeyManagers = new KeyManagersParameters();
        mtlsParams.setKeyManagers(newKeyManagers);
        assertEquals(newKeyManagers, mtlsParams.getKeyManagers());
    }

    @Test
    void testGetSetTrustManagers() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        TrustManagersParameters trustManagers = mtlsParams.getTrustManagers();
        assertNotNull(trustManagers);
        assertEquals(baseParams.getTrustManagers(), trustManagers);

        TrustManagersParameters newTrustManagers = new TrustManagersParameters();
        mtlsParams.setTrustManagers(newTrustManagers);
        assertEquals(newTrustManagers, mtlsParams.getTrustManagers());
    }

    @Test
    void testGetSetSecureSocketProtocols() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SecureSocketProtocolsParameters protocols = new SecureSocketProtocolsParameters();
        protocols.getSecureSocketProtocol().add("TLSv1.3");
        mtlsParams.setSecureSocketProtocols(protocols);

        SecureSocketProtocolsParameters result = mtlsParams.getSecureSocketProtocols();
        assertNotNull(result);
        assertEquals(protocols, result);
    }

    @Test
    void testGetSetSecureSocketProtocol() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        mtlsParams.setSecureSocketProtocol("TLSv1.3");
        assertEquals("TLSv1.3", mtlsParams.getSecureSocketProtocol());
    }

    @Test
    void testGetSetCipherSuites() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        CipherSuitesParameters cipherSuites = new CipherSuitesParameters();
        cipherSuites.getCipherSuite().add("TLS_AES_256_GCM_SHA384");
        mtlsParams.setCipherSuites(cipherSuites);

        CipherSuitesParameters result = mtlsParams.getCipherSuites();
        assertNotNull(result);
        assertEquals(cipherSuites, result);
    }

    @Test
    void testGetSetProvider() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        mtlsParams.setProvider("SunJSSE");
        assertEquals("SunJSSE", mtlsParams.getProvider());
    }

    @Test
    void testGetSetSecureRandom() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SecureRandomParameters secureRandom = new SecureRandomParameters();
        mtlsParams.setSecureRandom(secureRandom);

        SecureRandomParameters result = mtlsParams.getSecureRandom();
        assertEquals(secureRandom, result);
    }

    @Test
    void testGetSetCertAlias() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        mtlsParams.setCertAlias("myAlias");
        assertEquals("myAlias", mtlsParams.getCertAlias());
    }

    @Test
    void testGetSetClientParameters() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContextClientParameters clientParams = new SSLContextClientParameters();
        mtlsParams.setClientParameters(clientParams);

        SSLContextClientParameters result = mtlsParams.getClientParameters();
        assertEquals(clientParams, result);
    }

    @Test
    void testGetSetServerParameters() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContextServerParameters serverParams = new SSLContextServerParameters();
        mtlsParams.setServerParameters(serverParams);

        SSLContextServerParameters result = mtlsParams.getServerParameters();
        assertEquals(serverParams, result);
    }

    @Test
    void testGetSetSessionTimeout() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        mtlsParams.setSessionTimeout("3600");
        assertEquals("3600", mtlsParams.getSessionTimeout());
    }

    @Test
    void testGetSetCamelContext() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        CamelContext newContext = new DefaultCamelContext();
        mtlsParams.setCamelContext(newContext);

        CamelContext result = mtlsParams.getCamelContext();
        assertEquals(newContext, result);
    }

    @Test
    void testSSLEngineWrapMethods() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();

        assertNotNull(engine.getSupportedCipherSuites());
        assertNotNull(engine.getEnabledCipherSuites());
        assertNotNull(engine.getSupportedProtocols());
        assertNotNull(engine.getEnabledProtocols());

        String[] originalCipherSuites = engine.getEnabledCipherSuites();
        engine.setEnabledCipherSuites(originalCipherSuites);
        assertArrayEquals(originalCipherSuites, engine.getEnabledCipherSuites());

        String[] originalProtocols = engine.getEnabledProtocols();
        engine.setEnabledProtocols(originalProtocols);
        assertArrayEquals(originalProtocols, engine.getEnabledProtocols());
    }

    @Test
    void testSSLEngineClientModeAndAuth() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();

        engine.setUseClientMode(true);
        assertTrue(engine.getUseClientMode());

        engine.setUseClientMode(false);
        assertFalse(engine.getUseClientMode());

        engine.setNeedClientAuth(true);
        assertTrue(engine.getNeedClientAuth());

        engine.setWantClientAuth(true);
        assertTrue(engine.getWantClientAuth());
    }

    @Test
    void testSSLEngineSessionCreation() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();

        engine.setEnableSessionCreation(true);
        assertTrue(engine.getEnableSessionCreation());

        engine.setEnableSessionCreation(false);
        assertFalse(engine.getEnableSessionCreation());
    }

    @Test
    void testSSLEngineSession() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();

        assertNotNull(engine.getSession());
    }

    @Test
    void testSSLEngineHandshakeStatus() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();

        assertNotNull(engine.getHandshakeStatus());
    }

    @Test
    void testSSLEngineInboundOutbound() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();

        assertFalse(engine.isInboundDone());
        assertFalse(engine.isOutboundDone());

        engine.closeOutbound();
        assertTrue(engine.isOutboundDone());
    }

    @Test
    void testSSLContextSessionContexts() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);

        assertNotNull(sslContext.getClientSessionContext());
        assertNotNull(sslContext.getServerSessionContext());
    }

    @Test
    void testSSLContextSocketFactories() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);

        assertNotNull(sslContext.getSocketFactory());
        assertNotNull(sslContext.getServerSocketFactory());
    }

    @Test
    void testSSLEngineBeginHandshake() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(true);

        assertDoesNotThrow(engine::beginHandshake);
    }

    @Test
    void testSSLContextProtocol() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        assertNotNull(sslContext.getProtocol());
    }

    @Test
    void testSSLContextProvider() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        assertNotNull(sslContext.getProvider());
    }

    @Test
    void testMtlsParametersWithNullValues() {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        assertDoesNotThrow(() -> mtlsParams.setSecureSocketProtocol(null));
        assertDoesNotThrow(() -> mtlsParams.setProvider(null));
        assertDoesNotThrow(() -> mtlsParams.setCertAlias(null));
        assertDoesNotThrow(() -> mtlsParams.setSessionTimeout(null));

        assertDoesNotThrow(mtlsParams::getSecureSocketProtocol);
        assertDoesNotThrow(mtlsParams::getProvider);
        assertDoesNotThrow(mtlsParams::getCertAlias);
        assertDoesNotThrow(mtlsParams::getSessionTimeout);
    }

    @Test
    void testGettersReturnNullWhenNotSet() {
        SSLContextParameters emptyParams = new SSLContextParameters();
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            emptyParams, true);

        assertNull(mtlsParams.getSecureSocketProtocols());
        assertNull(mtlsParams.getCipherSuites());
        assertNull(mtlsParams.getSecureRandom());
        assertNull(mtlsParams.getClientParameters());
        assertNull(mtlsParams.getServerParameters());
    }

    @Test
    void testSSLEngineWithServerMode() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();

        engine.setUseClientMode(false);
        assertFalse(engine.getUseClientMode());

        engine.setNeedClientAuth(true);
        assertTrue(engine.getNeedClientAuth());
    }

    @Test
    void testCreateSSLContextWithBothFlags() throws Exception {
        MtlsAwareSSLContextParameters mtlsParamsStrict = new MtlsAwareSSLContextParameters(
            baseParams, true);

        SSLContext sslContext = mtlsParamsStrict.createSSLContext(camelContext);
        assertNotNull(sslContext);

        MtlsAwareSSLContextParameters mtlsParamsPermissive = new MtlsAwareSSLContextParameters(
            baseParams, false);

        SSLContext sslContext2 = mtlsParamsPermissive.createSSLContext(camelContext);
        assertNotNull(sslContext2);
    }
}
