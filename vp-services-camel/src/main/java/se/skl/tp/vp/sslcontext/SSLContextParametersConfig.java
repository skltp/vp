package se.skl.tp.vp.sslcontext;

import org.apache.camel.util.jsse.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import se.skl.tp.vp.config.SecurityProperties;

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
        return sslContextParameters;
    }

    private SecureSocketProtocolsParameters createSecureProtocolParameters(String allowedProtocolsString) {
        SecureSocketProtocolsParameters sspp = new SecureSocketProtocolsParameters();
        List<String> allowedProtocols = new ArrayList<>();
        for (String protocol: allowedProtocolsString.split(DELIMITER)) {
            if(!protocol.trim().isEmpty()){
                allowedProtocols.add(protocol);
            }
        }
        sspp.setSecureSocketProtocol(allowedProtocols);
        return sspp;
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
