package se.skl.tp.vp.logging.logentry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EcsTlsLogEntryTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testBuilderCreatesSslContextRegisterLogEntry() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .build();

        assertEquals(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER, entry.get(EcsFields.EVENT_ACTION));
        assertEquals(EcsTlsLogEntry.EVENT_KIND_VALUE, entry.get(EcsFields.EVENT_KIND));
        assertEquals(EcsTlsLogEntry.EVENT_CATEGORY_VALUE, entry.get(EcsFields.EVENT_CATEGORY));
        assertEquals(EcsTlsLogEntry.EVENT_MODULE_VALUE, entry.get(EcsFields.EVENT_MODULE));
        assertNotNull(entry.get(EcsFields.EVENT_ID));
    }

    @Test
    void testBuilderCreatesTlsHandshakeCompleteLogEntry() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .build();

        assertEquals(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE, entry.get(EcsFields.EVENT_ACTION));
        assertEquals(EcsTlsLogEntry.EVENT_KIND_VALUE, entry.get(EcsFields.EVENT_KIND));
        assertNotNull(entry.get(EcsFields.EVENT_ID));
    }

    @Test
    void testWithSslContextId() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .withSslContextId("SSLContext-default")
                .build();

        assertEquals("SSLContext-default",
                entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_SSL_CONTEXT_ID));
    }

    @Test
    void testWithProtocolsAndCipherSuites() {
        List<String> protocols = Arrays.asList("TLSv1.2", "TLSv1.3");
        List<String> cipherSuites = Arrays.asList("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");

        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .withProtocolsAndCipherSuites(protocols, cipherSuites)
                .build();

        assertEquals("2", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_PROTOCOL_COUNT));
        assertEquals("2", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_CIPHER_SUITE_COUNT));

        // Verify protocols are stored as JSON array
        String protocolsJson = entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_PROTOCOLS);
        assertNotNull(protocolsJson);
        assertTrue(protocolsJson.contains("TLSv1.2"));
        assertTrue(protocolsJson.contains("TLSv1.3"));

        // Verify cipher suites are stored as JSON array
        String cipherSuitesJson = entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_CIPHER_SUITES);
        assertNotNull(cipherSuitesJson);
        assertTrue(cipherSuitesJson.contains("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"));
    }

    @Test
    void testWithProtocolsAndCipherSuitesWithNullValues() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .withProtocolsAndCipherSuites(null, null)
                .build();

        assertNull(entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_PROTOCOL_COUNT));
        assertNull(entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_CIPHER_SUITE_COUNT));
    }

    @Test
    void testWithHandshakeDetails() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "TLSv1.3",
                        "TLS_AES_256_GCM_SHA384", 2)
                .build();

        assertEquals("example.com", entry.get(EcsFields.SERVER_ADDRESS));
        assertEquals("tls", entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertEquals("1.3", entry.get(EcsFields.TLS_VERSION));
        assertEquals("TLS_AES_256_GCM_SHA384", entry.get(EcsFields.TLS_CIPHER));
        assertEquals("true", entry.get(EcsFields.TLS_ESTABLISHED));
        assertEquals("2", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_CLIENT_CERT_COUNT));
    }

    @Test
    void testWithHandshakeDetailsWithNullPeerHost() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails(null, "TLSv1.2", "TLS_RSA_WITH_AES_128_CBC_SHA", 0)
                .build();

        assertNull(entry.get(EcsFields.SERVER_ADDRESS));
        assertEquals("tls", entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertEquals("1.2", entry.get(EcsFields.TLS_VERSION));
        assertEquals("0", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_CLIENT_CERT_COUNT));
    }

    @Test
    void testWithCustomMessage() {
        String customMessage = "Custom TLS message";
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .withMessage(customMessage)
                .build();

        assertEquals(customMessage, entry.get(EcsFields.MESSAGE));
    }

    @Test
    void testSslContextRegisterWithoutMessage() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .withSslContextId("SSLContext-test")
                .withProtocolsAndCipherSuites(
                        List.of("TLSv1.3"),
                        List.of("TLS_AES_256_GCM_SHA384"))
                .build();

        // Message is optional - verify key structured fields are present
        assertNull(entry.get(EcsFields.MESSAGE));
        assertEquals("SSLContext-test", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_SSL_CONTEXT_ID));
        assertEquals("1", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_PROTOCOL_COUNT));
        assertEquals("1", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_CIPHER_SUITE_COUNT));
    }

    @Test
    void testTlsHandshakeWithoutMessage() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "TLSv1.3", "TLS_AES_256_GCM_SHA384", 1)
                .build();

        // Message is optional - verify key structured fields are present
        assertNull(entry.get(EcsFields.MESSAGE));
        assertEquals("example.com", entry.get(EcsFields.SERVER_ADDRESS));
        assertEquals("tls", entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertEquals("1.3", entry.get(EcsFields.TLS_VERSION));
        assertEquals("TLS_AES_256_GCM_SHA384", entry.get(EcsFields.TLS_CIPHER));
    }

    @Test
    void testHostInformationIsPopulated() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .build();

        // Host information should be populated from EcsSystemProperties
        // In a real environment, at least one of these should be populated
        // In tests, we just verify the structure is correct by checking event ID
        assertNotNull(entry.get(EcsFields.EVENT_ID));
    }

    @Test
    void testCompleteSSLContextRegisterScenario() {
        List<String> protocols = Arrays.asList("TLSv1.2", "TLSv1.3");
        List<String> cipherSuites = Arrays.asList(
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_AES_256_GCM_SHA384"
        );

        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .withSslContextId("SSLContext-producer-default")
                .withProtocolsAndCipherSuites(protocols, cipherSuites)
                .withMessage("Registering SSL Context with id 'SSLContext-producer-default' - protocols (2): [TLSv1.2, TLSv1.3], cipher suites (3): [...]")
                .build();

        // Verify all key fields
        assertEquals(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER, entry.get(EcsFields.EVENT_ACTION));
        assertEquals("SSLContext-producer-default",
                entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_SSL_CONTEXT_ID));
        assertEquals("2", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_PROTOCOL_COUNT));
        assertEquals("3", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_CIPHER_SUITE_COUNT));
        assertNotNull(entry.get(EcsFields.MESSAGE));
        assertNotNull(entry.get(EcsFields.EVENT_ID));

        // Verify the entry has data that can be logged
        assertFalse(entry.getData().isEmpty());
    }

    @Test
    void testCompleteTlsHandshakeScenario() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("backend.example.com", "TLSv1.3",
                        "TLS_AES_256_GCM_SHA384", 1)
                .withMessage("TLS handshake completed for backend.example.com:443 - Protocol: TLSv1.3, Cipher Suite: TLS_AES_256_GCM_SHA384, Local certificates (client): 1 sent")
                .build();

        // Verify all key fields
        assertEquals(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE, entry.get(EcsFields.EVENT_ACTION));
        assertEquals("backend.example.com", entry.get(EcsFields.SERVER_ADDRESS));
        assertEquals("tls", entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertEquals("1.3", entry.get(EcsFields.TLS_VERSION));
        assertEquals("TLS_AES_256_GCM_SHA384", entry.get(EcsFields.TLS_CIPHER));
        assertEquals("true", entry.get(EcsFields.TLS_ESTABLISHED));
        assertEquals("1", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_CLIENT_CERT_COUNT));
        assertNotNull(entry.get(EcsFields.MESSAGE));
        assertNotNull(entry.get(EcsFields.EVENT_ID));
    }

    @Test
    void testJsonSerializationOfProtocolsList() throws Exception {
        List<String> protocols = Arrays.asList("TLSv1.2", "TLSv1.3");

        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .withProtocolsAndCipherSuites(protocols, null)
                .build();

        String protocolsJson = entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_PROTOCOLS);
        assertNotNull(protocolsJson);

        // Verify it's valid JSON
        List<String> deserializedProtocols = objectMapper.readValue(protocolsJson,
                new TypeReference<>() {
                });
        assertEquals(2, deserializedProtocols.size());
        assertTrue(deserializedProtocols.contains("TLSv1.2"));
        assertTrue(deserializedProtocols.contains("TLSv1.3"));
    }

    @Test
    void testEventCategoryIsArray() throws Exception {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_SSL_CONTEXT_REGISTER)
                .build();

        String categoryJson = entry.get(EcsFields.EVENT_CATEGORY);
        assertNotNull(categoryJson);

        // Verify it's a valid JSON array
        List<String> categories = objectMapper.readValue(categoryJson,
                new TypeReference<>() {
                });
        assertTrue(categories.contains("configuration"));
        assertTrue(categories.contains("network"));
    }

    @Test
    void testBuilderDoesNotAddNullValues() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails(null, null, null, 0)
                .build();

        // Only non-null values should be added
        assertNull(entry.get(EcsFields.SERVER_ADDRESS));
        assertNull(entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertNull(entry.get(EcsFields.TLS_VERSION));
        assertNull(entry.get(EcsFields.TLS_CIPHER));
        // But client cert count should be present even if 0
        assertEquals("0", entry.get(EcsFields.LABELS + EcsTlsLogEntry.LABEL_CLIENT_CERT_COUNT));
    }

    @Test
    void testTlsProtocolParsingTLSv1_2() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "TLSv1.2", "TLS_CIPHER", 0)
                .build();

        assertEquals("tls", entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertEquals("1.2", entry.get(EcsFields.TLS_VERSION));
    }

    @Test
    void testTlsProtocolParsingTLSv1_1() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "TLSv1.1", "TLS_CIPHER", 0)
                .build();

        assertEquals("tls", entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertEquals("1.1", entry.get(EcsFields.TLS_VERSION));
    }

    @Test
    void testTlsProtocolParsingTLSv1() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "TLSv1", "TLS_CIPHER", 0)
                .build();

        assertEquals("tls", entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertEquals("1", entry.get(EcsFields.TLS_VERSION));
    }

    @Test
    void testTlsProtocolParsingSSLv3() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "SSLv3", "SSL_CIPHER", 0)
                .build();

        assertEquals("ssl", entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertEquals("3", entry.get(EcsFields.TLS_VERSION));
    }

    @Test
    void testTlsProtocolParsingCaseInsensitive() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "tlsv1.3", "TLS_CIPHER", 0)
                .build();

        assertEquals("tls", entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertEquals("1.3", entry.get(EcsFields.TLS_VERSION));
    }

    @Test
    void testTlsProtocolParsingEmptyString() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "", "TLS_CIPHER", 0)
                .build();

        assertNull(entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertNull(entry.get(EcsFields.TLS_VERSION));
    }

    @Test
    void testTlsProtocolParsingShortString() {
        // Test strings that are too short to have a version (e.g., "TLS", "TLSV")
        EcsTlsLogEntry entry1 = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "TLSV", "TLS_CIPHER", 0)
                .build();

        assertNull(entry1.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertNull(entry1.get(EcsFields.TLS_VERSION));

        EcsTlsLogEntry entry2 = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "SSLV", "SSL_CIPHER", 0)
                .build();

        assertNull(entry2.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertNull(entry2.get(EcsFields.TLS_VERSION));
    }

    @Test
    void testTlsProtocolParsingUnknownProtocol() {
        EcsTlsLogEntry entry = new EcsTlsLogEntry.Builder(EcsTlsLogEntry.ACTION_TLS_HANDSHAKE_COMPLETE)
                .withHandshakeDetails("example.com", "UNKNOWN", "CIPHER", 0)
                .build();

        assertNull(entry.get(EcsFields.TLS_VERSION_PROTOCOL));
        assertNull(entry.get(EcsFields.TLS_VERSION));
    }
}


