package se.skl.tp.vp.logging.logentry;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * ECS-compliant log entry for TLS/SSL related events.
 */
@Slf4j
public class EcsTlsLogEntry extends BaseEcsLogEntry {

    public static final String EVENT_MODULE_VALUE = "skltp-tls";
    public static final String EVENT_CATEGORY_VALUE;

    static {
        try {
            EVENT_CATEGORY_VALUE = OBJECT_MAPPER.writeValueAsString(List.of("configuration", "network"));
        } catch (JsonProcessingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Event action constants
    public static final String ACTION_SSL_CONTEXT_REGISTER = "ssl-context-register";
    public static final String ACTION_TLS_HANDSHAKE_COMPLETE = "tls-handshake-complete";

    // Custom label fields for TLS events
    public static final String LABEL_SSL_CONTEXT_ID = "sslContextId";
    public static final String LABEL_PROTOCOL_COUNT = "protocolCount";
    public static final String LABEL_CIPHER_SUITE_COUNT = "cipherSuiteCount";
    public static final String LABEL_CLIENT_CERT_COUNT = "clientCertCount";
    public static final String LABEL_PROTOCOLS = "protocols";
    public static final String LABEL_CIPHER_SUITES = "cipherSuites";

    public static class Builder extends BaseBuilder<EcsTlsLogEntry, Builder> {

        public Builder(String eventAction) {
            super(eventAction, EVENT_KIND_VALUE, EVENT_CATEGORY_VALUE, EVENT_MODULE_VALUE);
            putData(EcsFields.EVENT_ID, UUID.randomUUID().toString());
        }

        /**
         * Set the SSL context ID (for context registration events)
         */
        public Builder withSslContextId(String sslContextId) {
            putLabel(LABEL_SSL_CONTEXT_ID, sslContextId);
            return self();
        }

        /**
         * Set the list of protocols and cipher suites (for context registration events)
         */
        public Builder withProtocolsAndCipherSuites(List<String> protocols, List<String> cipherSuites) {
            if (protocols != null) {
                putLabel(LABEL_PROTOCOL_COUNT, String.valueOf(protocols.size()));
                try {
                    putLabel(LABEL_PROTOCOLS, OBJECT_MAPPER.writeValueAsString(protocols));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize protocols list", e);
                    putLabel(LABEL_PROTOCOLS, protocols.toString());
                }
            }
            if (cipherSuites != null) {
                putLabel(LABEL_CIPHER_SUITE_COUNT, String.valueOf(cipherSuites.size()));
                try {
                    putLabel(LABEL_CIPHER_SUITES, OBJECT_MAPPER.writeValueAsString(cipherSuites));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize cipher suites list", e);
                    putLabel(LABEL_CIPHER_SUITES, cipherSuites.toString());
                }
            }
            return self();
        }

        /**
         * Parse TLS/SSL protocol string (e.g., "TLSv1.3") and set both tls.version_protocol and tls.version fields.
         * @param protocolString Protocol string from SSL session (e.g., "TLSv1.3", "TLSv1.2", "SSLv3")
         */
        private void parseTlsProtocol(String protocolString) {
            if (protocolString == null || protocolString.isEmpty()) {
                return;
            }

            // Parse protocol strings like "TLSv1.3", "TLSv1.2", "SSLv3", etc.
            // ECS expects: tls.version_protocol = normalized lowercase protocol (e.g., "tls", "ssl")
            //              tls.version = numeric version part (e.g., "1.3", "1.2")
            String normalizedProtocol = null;
            String version = null;

            String upper = protocolString.toUpperCase();
            if (upper.startsWith("TLSV") && protocolString.length() > 4) {
                normalizedProtocol = "tls";
                version = protocolString.substring(4); // Extract version part after "TLSv"
            } else if (upper.startsWith("SSLV") && protocolString.length() > 4) {
                normalizedProtocol = "ssl";
                version = protocolString.substring(4); // Extract version part after "SSLv"
            } else {
                log.warn("Unrecognized TLS/SSL protocol string: {}", protocolString);
            }

            if (normalizedProtocol != null) {
                putData(EcsFields.TLS_VERSION_PROTOCOL, normalizedProtocol);
            }
            if (version != null) {
                putData(EcsFields.TLS_VERSION, version);
            }
        }

        /**
         * Set TLS handshake details (for handshake completion events)
         */
        public Builder withHandshakeDetails(String peerHost, String protocol, String cipherSuite, int clientCertCount) {
            putData(EcsFields.SERVER_ADDRESS, peerHost);
            parseTlsProtocol(protocol);
            putData(EcsFields.TLS_CIPHER, cipherSuite);
            putData(EcsFields.TLS_ESTABLISHED, 0 < clientCertCount ? "true" : "false");
            putLabel(LABEL_CLIENT_CERT_COUNT, String.valueOf(clientCertCount));
            return self();
        }

        /**
         * Set a custom message
         */
        public Builder withMessage(String message) {
            putData(EcsFields.MESSAGE, message);
            return self();
        }

        @Override
        public EcsTlsLogEntry build() {
            EcsTlsLogEntry entry = new EcsTlsLogEntry();
            entry.putAll(data.getData());
            return entry;
        }
    }
}
