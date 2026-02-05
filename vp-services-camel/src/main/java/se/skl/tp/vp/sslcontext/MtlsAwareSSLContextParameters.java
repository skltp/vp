package se.skl.tp.vp.sslcontext;

import lombok.extern.log4j.Log4j2;
import org.apache.camel.CamelContext;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.camel.support.jsse.SecureSocketProtocolsParameters;
import org.apache.camel.support.jsse.CipherSuitesParameters;
import org.apache.camel.support.jsse.SecureRandomParameters;
import org.apache.camel.support.jsse.SSLContextClientParameters;
import org.apache.camel.support.jsse.SSLContextServerParameters;
import se.skl.tp.vp.logging.logentry.EcsTlsLogEntry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

/**
 * Custom SSLContextParameters that wraps SSL engines to monitor mTLS (mutual TLS) certificate exchange.
 *
 * This class extends Camel's SSLContextParameters to inject mTLS verification logic without requiring
 * a clientInitializerFactory (which is ignored when sslContextParameters is used).
 *
 * The wrapper intercepts SSLEngine creation and adds a handshake listener that verifies whether
 * client certificates were actually sent during TLS negotiation.
 */
@Log4j2
public class MtlsAwareSSLContextParameters extends SSLContextParameters {

    private final SSLContextParameters delegate;
    private final boolean mtlsVerificationEnabled;

    public MtlsAwareSSLContextParameters(SSLContextParameters delegate,
                                         boolean mtlsVerificationEnabled) {
        this.delegate = delegate;
        this.mtlsVerificationEnabled = mtlsVerificationEnabled;

        if (log.isDebugEnabled()) {
            log.debug("Created MtlsAwareSSLContextParameters: enabled={}",
                mtlsVerificationEnabled);
        }
    }

    @Override
    public SSLContext createSSLContext(CamelContext camelContext) throws GeneralSecurityException, IOException {
        SSLContext sslContext = delegate.createSSLContext(camelContext);

        if (!mtlsVerificationEnabled) {
            return sslContext;
        }

        // Wrap the SSLContext to intercept SSLEngine creation
        return new MtlsAwareSSLContext(sslContext, mtlsVerificationEnabled);
    }

    // Delegate all getter/setter methods to the delegate

    @Override
    public KeyManagersParameters getKeyManagers() {
        return delegate.getKeyManagers();
    }

    @Override
    public void setKeyManagers(KeyManagersParameters keyManagers) {
        delegate.setKeyManagers(keyManagers);
    }

    @Override
    public TrustManagersParameters getTrustManagers() {
        return delegate.getTrustManagers();
    }

    @Override
    public void setTrustManagers(TrustManagersParameters trustManagers) {
        delegate.setTrustManagers(trustManagers);
    }

    @Override
    public SecureSocketProtocolsParameters getSecureSocketProtocols() {
        return delegate.getSecureSocketProtocols();
    }

    @Override
    public void setSecureSocketProtocols(SecureSocketProtocolsParameters secureSocketProtocols) {
        delegate.setSecureSocketProtocols(secureSocketProtocols);
    }

    @Override
    public String getSecureSocketProtocol() {
        return delegate.getSecureSocketProtocol();
    }

    @Override
    public void setSecureSocketProtocol(String secureSocketProtocol) {
        delegate.setSecureSocketProtocol(secureSocketProtocol);
    }

    @Override
    public CipherSuitesParameters getCipherSuites() {
        return delegate.getCipherSuites();
    }

    @Override
    public void setCipherSuites(CipherSuitesParameters cipherSuites) {
        delegate.setCipherSuites(cipherSuites);
    }

    @Override
    public String getProvider() {
        return delegate.getProvider();
    }

    @Override
    public void setProvider(String provider) {
        delegate.setProvider(provider);
    }

    @Override
    public SecureRandomParameters getSecureRandom() {
        return delegate.getSecureRandom();
    }

    @Override
    public void setSecureRandom(SecureRandomParameters secureRandom) {
        delegate.setSecureRandom(secureRandom);
    }

    @Override
    public String getCertAlias() {
        return delegate.getCertAlias();
    }

    @Override
    public void setCertAlias(String certAlias) {
        delegate.setCertAlias(certAlias);
    }

    @Override
    public SSLContextClientParameters getClientParameters() {
        return delegate.getClientParameters();
    }

    @Override
    public void setClientParameters(SSLContextClientParameters clientParameters) {
        delegate.setClientParameters(clientParameters);
    }

    @Override
    public SSLContextServerParameters getServerParameters() {
        return delegate.getServerParameters();
    }

    @Override
    public void setServerParameters(SSLContextServerParameters serverParameters) {
        delegate.setServerParameters(serverParameters);
    }

    @Override
    public String getSessionTimeout() {
        return delegate.getSessionTimeout();
    }

    @Override
    public void setSessionTimeout(String sessionTimeout) {
        delegate.setSessionTimeout(sessionTimeout);
    }

    @Override
    public CamelContext getCamelContext() {
        return delegate.getCamelContext();
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        delegate.setCamelContext(camelContext);
    }

    /**
     * Wrapper SSLContext that creates wrapped SSLEngines with mTLS verification.
     */
    private static class MtlsAwareSSLContext extends SSLContext {

        protected MtlsAwareSSLContext(SSLContext delegate,
                                      boolean mtlsVerificationEnabled) {
            super(new MtlsAwareSSLContextSpi(delegate, mtlsVerificationEnabled),
                  delegate.getProvider(),
                  delegate.getProtocol());
        }
    }

    /**
     * SSLContextSpi implementation that wraps SSLEngines with mTLS verification.
     */
    private static class MtlsAwareSSLContextSpi extends SSLContextSpi {
        private final SSLContext delegate;
        private final boolean mtlsVerificationEnabled;

        public MtlsAwareSSLContextSpi(SSLContext delegate,
                                      boolean mtlsVerificationEnabled) {
            this.delegate = delegate;
            this.mtlsVerificationEnabled = mtlsVerificationEnabled;
        }

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            SSLEngine engine = delegate.createSSLEngine();
            return wrapEngine(engine);
        }

        @Override
        protected SSLEngine engineCreateSSLEngine(String host, int port) {
            SSLEngine engine = delegate.createSSLEngine(host, port);
            return wrapEngine(engine);
        }

        private SSLEngine wrapEngine(SSLEngine engine) {
            if (!mtlsVerificationEnabled) {
                return engine;
            }
            return new MtlsVerifyingSSLEngine(engine);
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            return delegate.getClientSessionContext();
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            return delegate.getServerSessionContext();
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            return delegate.getServerSocketFactory();
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return delegate.getSocketFactory();
        }

        @Override
        protected void engineInit(KeyManager[] km,
                                 TrustManager[] tm,
                                 SecureRandom sr) {
            // No-op, delegate is already initialized
        }
    }

    /**
     * SSLEngine wrapper that monitors handshake completion and verifies mTLS certificate exchange.
     */
    private static class MtlsVerifyingSSLEngine extends SSLEngine {
        public static final String TLS_HANDSHAKE_COMPLETED = "TLS handshake completed for %s - Protocol: %s, Cipher Suite: %s, Local certificates (client): %d sent";
        public static final String TLS_HANDSHAKE_FAILED = "TLS handshake failed for %s - Protocol: %s, Cipher Suite: %s, NO local certificates (client) sent";
        private final SSLEngine delegate;
        private boolean handshakeChecked = false;

        public MtlsVerifyingSSLEngine(SSLEngine delegate) {
            this.delegate = delegate;
        }

        @Override
        public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length,
                                     ByteBuffer dst) throws SSLException {
            SSLEngineResult result = delegate.wrap(srcs, offset, length, dst);
            checkHandshakeStatus(result);
            return result;
        }

        @Override
        public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts,
                                       int offset, int length) throws SSLException {
            SSLEngineResult result = delegate.unwrap(src, dsts, offset, length);
            checkHandshakeStatus(result);
            return result;
        }

        private void checkHandshakeStatus(SSLEngineResult result) {
            log.trace("Handshake checked: {}. SSLEngine handshake status: {}", handshakeChecked, result.getHandshakeStatus());
            if (!handshakeChecked && result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                handshakeChecked = true;
                verifyMtls();
            }
        }

        private void verifyMtls() {
            try {
                SSLSession session = delegate.getSession();
                if (session == null || session.getPeerHost() == null || session.getProtocol() == null || session.getCipherSuite() == null) {
                    log.debug("SSLSession is null or incomplete, cannot verify mTLS");
                    return;
                }

                Certificate[] localCerts = session.getLocalCertificates();
                boolean mtlsUsed = localCerts != null && localCerts.length > 0;
                int certCount = localCerts != null ? localCerts.length : 0;

                if (log.isTraceEnabled()) {
                    logCertificateDetails(session);
                }

                var builder = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                        .withHandshakeDetails(
                                session.getPeerHost(),
                                session.getProtocol(),
                                session.getCipherSuite(),
                                certCount);

                if (mtlsUsed) {
                    log.info(
                        builder
                            .withMessage(String.format(TLS_HANDSHAKE_COMPLETED,
                                session.getPeerHost(),
                                session.getProtocol(),
                                session.getCipherSuite(),
                                certCount))
                            .build()
                    );
                } else {
                    log.error(
                        builder
                            .withMessage(String.format(TLS_HANDSHAKE_FAILED,
                                session.getPeerHost(),
                                session.getProtocol(),
                                session.getCipherSuite()))
                            .build()
                    );
                }
            } catch (Exception e) {
                log.error("Error during mTLS verification: {}", e.getMessage(), e);
            }
        }

        /**
         * Logs detailed certificate information at TRACE level for debugging purposes.
         *
         * @param session The SSL session containing peer certificates
         */
        private void logCertificateDetails(SSLSession session) {
            try {
                // Log local certificates (client certificates)
                Certificate[] localCerts = session.getLocalCertificates();
                if (localCerts != null && localCerts.length > 0) {
                    log.trace("Local certificates (client) sent: {} certificate(s)", localCerts.length);
                    for (int i = 0; i < localCerts.length; i++) {
                        Certificate cert = localCerts[i];
                        log.trace("  Local certificate [{}]: Type={}", i, cert.getType());
                        logCertificateDetails("Local", i, cert);
                    }
                } else {
                    log.trace("No local certificates (client) sent");
                }

                // Log peer certificates (server certificates)
                Certificate[] peerCerts = session.getPeerCertificates();
                if (peerCerts != null && peerCerts.length > 0) {
                    log.trace("Peer certificates (server) received: {} certificate(s)", peerCerts.length);
                    for (int i = 0; i < peerCerts.length; i++) {
                        Certificate cert = peerCerts[i];
                        log.trace("  Peer certificate [{}]: Type={}", i, cert.getType());
                        logCertificateDetails("Peer", i, cert);
                    }
                } else {
                    log.trace("No peer certificates (server) received");
                }
            } catch (javax.net.ssl.SSLPeerUnverifiedException e) {
                log.trace("Peer certificates not verified: {}", e.getMessage());
            } catch (Exception e) {
                log.trace("Error logging certificate details: {}", e.getMessage(), e);
            }
        }

        /**
         * Logs detailed information for an individual certificate.
         * For X.509 certificates, extracts Subject, Issuer, validity dates, serial number, and fingerprint.
         *
         * @param certType "Local" or "Peer" to identify certificate origin in logs
         * @param index Certificate index in the chain
         * @param cert The certificate to log details for
         */
        private void logCertificateDetails(String certType, int index, Certificate cert) {
            try {
                if (cert instanceof X509Certificate x509) {
                    log.trace("    {} cert [{}] Subject: {}", certType, index, x509.getSubjectX500Principal().getName());
                    log.trace("    {} cert [{}] Issuer:  {}", certType, index, x509.getIssuerX500Principal().getName());
                    log.trace("    {} cert [{}] Valid from {} to {}", certType, index, x509.getNotBefore(), x509.getNotAfter());
                    log.trace("    {} cert [{}] Serial: {}", certType, index, x509.getSerialNumber());

                    // Log SHA-256 fingerprint for certificate identification
                    logCertificateFingerprint(certType, index, x509);
                } else {
                    // Fallback for non-X.509 certificates
                    log.trace("    {} cert [{}] Details: {}", certType, index, cert.toString());
                }
            } catch (Exception e) {
                log.trace("    {} cert [{}] Error logging details: {}", certType, index, e.getMessage());
            }
        }

        /**
         * Logs the SHA-256 fingerprint of an X.509 certificate for identification purposes.
         *
         * @param certType "Local" or "Peer" to identify certificate origin in logs
         * @param index Certificate index in the chain
         * @param x509 The X.509 certificate to compute fingerprint for
         */
        private void logCertificateFingerprint(String certType, int index, X509Certificate x509) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] fingerprint = digest.digest(x509.getEncoded());
                String fingerprintHex = HexFormat.of().withUpperCase().withDelimiter(":").formatHex(fingerprint);
                log.trace("    {} cert [{}] SHA-256: {}", certType, index, fingerprintHex);
            } catch (Exception e) {
                log.trace("    {} cert [{}] Could not compute fingerprint: {}", certType, index, e.getMessage());
            }
        }

        // Delegate all other methods to the wrapped engine
        @Override
        public String getPeerHost() {
            return delegate.getPeerHost();
        }

        @Override
        public int getPeerPort() {
            return delegate.getPeerPort();
        }

        @Override
        public SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst)
                throws SSLException {
            SSLEngineResult result = delegate.wrap(src, dst);
            checkHandshakeStatus(result);
            return result;
        }

        @Override
        public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst)
                throws SSLException {
            SSLEngineResult result = delegate.unwrap(src, dst);
            checkHandshakeStatus(result);
            return result;
        }

        @Override
        public Runnable getDelegatedTask() {
            return delegate.getDelegatedTask();
        }

        @Override
        public void closeInbound() throws SSLException {
            delegate.closeInbound();
        }

        @Override
        public boolean isInboundDone() {
            return delegate.isInboundDone();
        }

        @Override
        public void closeOutbound() {
            delegate.closeOutbound();
        }

        @Override
        public boolean isOutboundDone() {
            return delegate.isOutboundDone();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public String[] getEnabledCipherSuites() {
            return delegate.getEnabledCipherSuites();
        }

        @Override
        public void setEnabledCipherSuites(String[] suites) {
            delegate.setEnabledCipherSuites(suites);
        }

        @Override
        public String[] getSupportedProtocols() {
            return delegate.getSupportedProtocols();
        }

        @Override
        public String[] getEnabledProtocols() {
            return delegate.getEnabledProtocols();
        }

        @Override
        public void setEnabledProtocols(String[] protocols) {
            delegate.setEnabledProtocols(protocols);
        }

        @Override
        public SSLSession getSession() {
            return delegate.getSession();
        }

        @Override
        public void beginHandshake() throws SSLException {
            delegate.beginHandshake();
        }

        @Override
        public SSLEngineResult.HandshakeStatus getHandshakeStatus() {
            return delegate.getHandshakeStatus();
        }

        @Override
        public void setUseClientMode(boolean mode) {
            delegate.setUseClientMode(mode);
        }

        @Override
        public boolean getUseClientMode() {
            return delegate.getUseClientMode();
        }

        @Override
        public void setNeedClientAuth(boolean need) {
            delegate.setNeedClientAuth(need);
        }

        @Override
        public boolean getNeedClientAuth() {
            return delegate.getNeedClientAuth();
        }

        @Override
        public void setWantClientAuth(boolean want) {
            delegate.setWantClientAuth(want);
        }

        @Override
        public boolean getWantClientAuth() {
            return delegate.getWantClientAuth();
        }

        @Override
        public void setEnableSessionCreation(boolean flag) {
            delegate.setEnableSessionCreation(flag);
        }

        @Override
        public boolean getEnableSessionCreation() {
            return delegate.getEnableSessionCreation();
        }

        @Override
        public SSLSession getHandshakeSession() {
            return delegate.getHandshakeSession();
        }
    }
}
