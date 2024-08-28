package se.skl.tp.vp.sslcontext;

import org.apache.camel.support.jsse.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import se.skl.tp.vp.config.SecurityProperties;

@Log4j2
@Configuration
public class SSLContextParametersConfig  {

    public static final String DELIMITER = ",";

    @Autowired
    SecurityProperties securityProperies;

    @Bean
    public SSLContextParameters incomingSSLContextParameters() {
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(securityProperies.getStore().getLocation() + securityProperies.getStore().getProducer().getFile());
        ksp.setPassword(securityProperies.getStore().getProducer().getPassword());
        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword(securityProperies.getStore().getProducer().getKeyPassword());
        kmp.setKeyStore(ksp);

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(kmp);

        TrustManagersParameters trustManagersParameters = createTrustManagerParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);

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
    public SSLContextParameters outgoingSSLContextParameters() {
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(securityProperies.getStore().getLocation() + securityProperies.getStore().getConsumer().getFile());
        ksp.setPassword(securityProperies.getStore().getConsumer().getPassword());
        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword(securityProperies.getStore().getConsumer().getKeyPassword());
        kmp.setKeyStore(ksp);

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(kmp);
        
        TrustManagersParameters trustManagersParameters = createTrustManagerParameters();
        sslContextParameters.setTrustManagers(trustManagersParameters);
        
        SecureSocketProtocolsParameters sspp = createSecureProtocolParameters(securityProperies.getAllowedOutgoingProtocols());
        sslContextParameters.setSecureSocketProtocols(sspp);

        // Set cipher suites
        if(!useAllCiphers(securityProperies.getAllowedOutgoingCipherSuites())) {
	        CipherSuitesParameters cipherSuites = createCipherSuiteParameters(securityProperies.getAllowedOutgoingCipherSuites());
			sslContextParameters.setCipherSuites(cipherSuites);
        }
        return sslContextParameters;
    }

    private boolean useAllCiphers(String s) {
    	return (s == null || s.trim().length() == 0 || s.trim().equals("*"));
    }
    
    private SecureSocketProtocolsParameters createSecureProtocolParameters(String allowedProtocolsString) {
        SecureSocketProtocolsParameters sspp = new SecureSocketProtocolsParameters();
        for (String protocol: allowedProtocolsString.split(DELIMITER)) {
            protocol.trim();
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
            protocol.trim();
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
