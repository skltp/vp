package se.skl.tp.vp.sslcontext;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jsse.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import java.nio.ByteBuffer;

import se.skl.tp.vp.sslcontext.MtlsAwareSSLContextParameters.MtlsVerifyingSSLEngine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MtlsAwareSSLContextParametersTest {

    private static final int MAX_HANDSHAKE_STEPS = 50;

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

    @Test
    void testNoMtlsVerificationBeforeHandshakeHasStarted() throws Exception {
        MtlsVerifyingSSLEngine engine = createClientEngine();

        // Netty may call wrap/unwrap before the handshake has actually started. The engine then
        // reports NOT_HANDSHAKING, which must not be interpreted as a completed handshake.
        ByteBuffer emptyNetworkData = ByteBuffer.allocate(0);
        ByteBuffer appBuffer = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
        engine.unwrap(emptyNetworkData, appBuffer);

        assertFalse(engine.isHandshakeChecked(), "mTLS must not be verified before the handshake started");
        assertNull(engine.getMtlsVerified());
    }

    @Test
    void testNotHandshakingBeforeHandshakeStartedDoesNotTriggerVerification() throws Exception {
        SSLEngine delegate = mockEngineWithUnnegotiatedSession(
            new SSLEngineResult(SSLEngineResult.Status.BUFFER_UNDERFLOW,
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0));

        MtlsVerifyingSSLEngine engine = new MtlsVerifyingSSLEngine(delegate);
        engine.unwrap(ByteBuffer.allocate(0), new ByteBuffer[]{ByteBuffer.allocate(1024)}, 0, 1);

        assertFalse(engine.isHandshakeChecked(),
            "NOT_HANDSHAKING before the handshake started must not be treated as a completed handshake");
        assertNull(engine.getMtlsVerified());
    }

    @Test
    void testNotHandshakingWithUnnegotiatedSessionDoesNotTriggerVerification() throws Exception {
        SSLEngine delegate = mockEngineWithUnnegotiatedSession(
            new SSLEngineResult(SSLEngineResult.Status.OK,
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP, 0, 0),
            new SSLEngineResult(SSLEngineResult.Status.OK,
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING, 0, 0));

        MtlsVerifyingSSLEngine engine = new MtlsVerifyingSSLEngine(delegate);
        engine.unwrap(ByteBuffer.allocate(0), new ByteBuffer[]{ByteBuffer.allocate(1024)}, 0, 1);
        engine.unwrap(ByteBuffer.allocate(0), new ByteBuffer[]{ByteBuffer.allocate(1024)}, 0, 1);

        assertFalse(engine.isHandshakeChecked(),
            "An unnegotiated session must not be reported as missing a client certificate");
        assertNull(engine.getMtlsVerified());
    }

    private SSLEngine mockEngineWithUnnegotiatedSession(SSLEngineResult first, SSLEngineResult... rest)
            throws Exception {
        SSLSession unnegotiatedSession = mock(SSLSession.class);
        when(unnegotiatedSession.getPeerHost()).thenReturn("producer.example.org");
        when(unnegotiatedSession.getProtocol()).thenReturn("NONE");
        when(unnegotiatedSession.getCipherSuite()).thenReturn("SSL_NULL_WITH_NULL_NULL");

        SSLEngine delegate = mock(SSLEngine.class);
        when(delegate.getSession()).thenReturn(unnegotiatedSession);
        when(delegate.unwrap(any(ByteBuffer.class), any(ByteBuffer[].class), anyInt(), anyInt()))
            .thenReturn(first, rest);
        return delegate;
    }

    @Test
    void testNoMtlsVerificationWhenEngineIsClosedBeforeHandshake() throws Exception {
        MtlsVerifyingSSLEngine engine = createClientEngine();

        engine.closeOutbound();
        ByteBuffer netBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        engine.wrap(ByteBuffer.allocate(0), netBuffer);

        assertFalse(engine.isHandshakeChecked(), "A closed engine must not trigger mTLS verification");
        assertNull(engine.getMtlsVerified());
    }

    @Test
    void testMtlsVerifiedAsUsedWhenClientCertificateIsSent() throws Exception {
        MtlsVerifyingSSLEngine client = createClientEngine();
        SSLEngine server = createServerEngine(true);

        doHandshake(client, server);

        assertTrue(client.isHandshakeChecked(), "mTLS should have been verified after a completed handshake");
        assertEquals(Boolean.TRUE, client.getMtlsVerified());
    }

    @Test
    void testMtlsVerifiedAsNotUsedWhenNoClientCertificateIsRequested() throws Exception {
        MtlsVerifyingSSLEngine client = createClientEngine();
        SSLEngine server = createServerEngine(false);

        doHandshake(client, server);

        assertTrue(client.isHandshakeChecked(), "mTLS should have been verified after a completed handshake");
        assertEquals(Boolean.FALSE, client.getMtlsVerified());
    }

    private MtlsVerifyingSSLEngine createClientEngine() throws Exception {
        MtlsAwareSSLContextParameters mtlsParams = new MtlsAwareSSLContextParameters(
            createHandshakeSSLContextParameters(), true);
        SSLContext sslContext = mtlsParams.createSSLContext(camelContext);
        // The host/port are only advisory hints used for session caching - no socket is opened and no
        // port is bound. All handshake data is exchanged in memory through wrap/unwrap below.
        SSLEngine engine = sslContext.createSSLEngine("localhost", 443);
        engine.setUseClientMode(true);
        return assertInstanceOf(MtlsVerifyingSSLEngine.class, engine);
    }

    private SSLEngine createServerEngine(boolean needClientAuth) throws Exception {
        SSLContext sslContext = createHandshakeSSLContextParameters().createSSLContext(camelContext);
        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(false);
        engine.setNeedClientAuth(needClientAuth);
        return engine;
    }

    /**
     * The legacy test certificates in tp.jks use a 1024-bit DSA key which modern JDKs refuse to use for
     * a TLS handshake. mtls-test.jks holds a self-signed RSA 2048/SHA256withRSA key pair (generated with
     * keytool, valid until 2046) that is used both as key material and as trust anchor for the
     * in-memory handshake tests.
     */
    private SSLContextParameters createHandshakeSSLContextParameters() {
        SSLContextParameters params = new SSLContextParameters();

        KeyStoreParameters keyStoreParams = new KeyStoreParameters();
        keyStoreParams.setResource("classpath:certs/mtls-test.jks");
        keyStoreParams.setPassword("password");

        KeyManagersParameters keyManagers = new KeyManagersParameters();
        keyManagers.setKeyStore(keyStoreParams);
        keyManagers.setKeyPassword("password");
        params.setKeyManagers(keyManagers);

        TrustManagersParameters trustManagers = new TrustManagersParameters();
        trustManagers.setKeyStore(keyStoreParams);
        params.setTrustManagers(trustManagers);

        return params;
    }

    /**
     * Runs a complete in-memory TLS handshake between the two engines.
     */
    private void doHandshake(SSLEngine client, SSLEngine server) throws Exception {
        SSLSession session = client.getSession();
        ByteBuffer clientToServer = ByteBuffer.allocate(session.getPacketBufferSize());
        ByteBuffer serverToClient = ByteBuffer.allocate(session.getPacketBufferSize());
        ByteBuffer clientApp = ByteBuffer.allocate(session.getApplicationBufferSize());
        ByteBuffer serverApp = ByteBuffer.allocate(session.getApplicationBufferSize());
        ByteBuffer empty = ByteBuffer.allocate(0);

        client.beginHandshake();
        server.beginHandshake();

        for (int i = 0; i < MAX_HANDSHAKE_STEPS; i++) {
            boolean progress = step(client, empty, clientToServer, serverToClient, clientApp);
            progress |= step(server, empty, serverToClient, clientToServer, serverApp);

            if (isHandshakeDone(client) && isHandshakeDone(server)) {
                // TLS 1.3 sends post-handshake messages (client certificate, session tickets) that
                // must be consumed before the local certificates are available on the session.
                step(client, empty, clientToServer, serverToClient, clientApp);
                step(server, empty, serverToClient, clientToServer, serverApp);
                return;
            }
            if (!progress) {
                fail("TLS handshake made no progress");
            }
        }
        fail("TLS handshake did not complete");
    }

    private boolean isHandshakeDone(SSLEngine engine) {
        SSLEngineResult.HandshakeStatus status = engine.getHandshakeStatus();
        return status == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING
            || status == SSLEngineResult.HandshakeStatus.FINISHED;
    }

    /**
     * Performs one wrap/unwrap/task step for the given engine.
     *
     * @param outbound buffer that the engine writes encrypted data to
     * @param inbound  buffer holding encrypted data written by the peer
     * @return true if the engine made any progress
     */
    private boolean step(SSLEngine engine, ByteBuffer emptyApp, ByteBuffer outbound,
                         ByteBuffer inbound, ByteBuffer appBuffer) throws Exception {
        boolean progress = false;
        for (int i = 0; i < MAX_HANDSHAKE_STEPS; i++) {
            switch (engine.getHandshakeStatus()) {
                case NEED_TASK -> {
                    Runnable task;
                    while ((task = engine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    progress = true;
                }
                case NEED_WRAP -> {
                    engine.wrap(emptyApp, outbound);
                    progress = true;
                }
                case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> {
                    inbound.flip();
                    if (!inbound.hasRemaining()) {
                        inbound.clear();
                        return progress;
                    }
                    engine.unwrap(inbound, appBuffer);
                    inbound.compact();
                    progress = true;
                }
                default -> {
                    // Consume any remaining post-handshake data from the peer
                    inbound.flip();
                    if (inbound.hasRemaining()) {
                        engine.unwrap(inbound, appBuffer);
                        progress = true;
                    }
                    inbound.compact();
                    return progress;
                }
            }
        }
        return progress;
    }
}
