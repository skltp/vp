package se.skl.tp.vp.sslcontext;

import org.apache.camel.support.jsse.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import se.skl.tp.vp.config.SecurityProperties;


import javax.net.ssl.*;

@Log4j2
@Configuration
public class SSLContextParametersConfig  {

    public static final String DELIMITER = ",";

    @Autowired
    SecurityProperties securityProperies;

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
        if(!useAllCiphers(securityProperies.getAllowedIncomingCipherSuites())) {
	        CipherSuitesParameters cipherSuites = createCipherSuiteParameters(securityProperies.getAllowedIncomingCipherSuites());
			sslContextParameters.setCipherSuites(cipherSuites);
        }
        return sslContextParameters;
    }

    @Bean
    public SSLContextParameters outgoingSSLContextParameters(SslBundles sslBundles, @Value("${tp.tls.store.consumer.bundle:}") String bundle) {
        SSLContextParameters sslContextParameters;
        if (bundle.isBlank()) {
            sslContextParameters = getSslContextParameters(securityProperies.getStore().getConsumer());
        } else {
            sslContextParameters = getBundleBasedParameters(sslBundles, bundle);
        }
        SecureSocketProtocolsParameters sspp = createSecureProtocolParameters(securityProperies.getAllowedOutgoingProtocols());
        sslContextParameters.setSecureSocketProtocols(sspp);

        // Set cipher suites
        if(!useAllCiphers(securityProperies.getAllowedOutgoingCipherSuites())) {
	        CipherSuitesParameters cipherSuites = createCipherSuiteParameters(securityProperies.getAllowedOutgoingCipherSuites());
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

    public static SSLContextParameters getBundleBasedParameters(SslBundles sslBundles, @Value("${tp.tls.store.consumer.bundle}") String bundle) {
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

    private boolean useAllCiphers(String s) {
    	return (s == null || s.trim().length() == 0 || s.trim().equals("*"));
    }
    
    private SecureSocketProtocolsParameters createSecureProtocolParameters(String allowedProtocolsString) {
        SecureSocketProtocolsParameters sspp = new SecureSocketProtocolsParameters();
        for (String protocol: allowedProtocolsString.split(DELIMITER)) {
            protocol = protocol.trim();
            if(!protocol.isEmpty()){
                sspp.getSecureSocketProtocol().add(protocol);
            }
        }
        return sspp;
    }

    private CipherSuitesParameters createCipherSuiteParameters(String cipherSuiteString) {
        CipherSuitesParameters cipherSuites = new CipherSuitesParameters();
        List<String> allowedCipherSuites = new ArrayList<>();
        for (String protocol: cipherSuiteString.split(DELIMITER)) {
            protocol = protocol.trim();
            if(!protocol.isEmpty()){
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
}
