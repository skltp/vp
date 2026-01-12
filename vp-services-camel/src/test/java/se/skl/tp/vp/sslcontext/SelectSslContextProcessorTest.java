package se.skl.tp.vp.sslcontext;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.skl.tp.vp.constants.VPExchangeProperties;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelectSslContextProcessorTest {

    @Mock
    private SSLContextService sslContextService;

    private SelectSslContextProcessor processor;
    private CamelContext camelContext;

    @BeforeEach
    void setUp() {
        processor = new SelectSslContextProcessor(sslContextService);
        camelContext = new DefaultCamelContext();
    }

    @ParameterizedTest
    @MethodSource("provideHostFormatsTestData")
    void shouldSetSslContextIdForDifferentHostFormats(String vagvalHost) throws Exception {
        Exchange exchange = createExchange();
        exchange.setProperty(VPExchangeProperties.VAGVAL_HOST, vagvalHost);
        when(sslContextService.getClientSSLContextId(vagvalHost)).thenReturn("expectedSslContextId");

        processor.process(exchange);

        assertEquals("expectedSslContextId", exchange.getProperty(VPExchangeProperties.SSL_CONTEXT_ID, String.class));
        verify(sslContextService, times(1)).getClientSSLContextId(vagvalHost);
    }

    private static Stream<Arguments> provideHostFormatsTestData() {
        return Stream.of(
            Arguments.of("example.com:8443"),
            Arguments.of("example.com"),
            Arguments.of("[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:8443")
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidHostTestData")
    void shouldThrowExceptionWhenVagvalHostIsNullOrBlank(String vagvalHost) {
        Exchange exchange = createExchange();
        exchange.setProperty(VPExchangeProperties.VAGVAL_HOST, vagvalHost);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> processor.process(exchange));

        assertEquals("Vagval not set in exchange properties", exception.getMessage());
        verify(sslContextService, never()).getClientSSLContextId(anyString());
    }

    private static Stream<Arguments> provideInvalidHostTestData() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(""),
                Arguments.of("  "),
                Arguments.of("\t")
        );
    }

    @Test
    void shouldHandleMultipleDifferentHosts() throws Exception {
        String host1 = "host1.example.com:8443";
        String host2 = "host2.example.com:9443";
        String sslContextId1 = "ssl-context-host1";
        String sslContextId2 = "ssl-context-host2";

        when(sslContextService.getClientSSLContextId(host1)).thenReturn(sslContextId1);
        when(sslContextService.getClientSSLContextId(host2)).thenReturn(sslContextId2);

        Exchange exchange1 = createExchange();
        exchange1.setProperty(VPExchangeProperties.VAGVAL_HOST, host1);
        processor.process(exchange1);

        Exchange exchange2 = createExchange();
        exchange2.setProperty(VPExchangeProperties.VAGVAL_HOST, host2);
        processor.process(exchange2);

        assertEquals(sslContextId1, exchange1.getProperty(VPExchangeProperties.SSL_CONTEXT_ID, String.class));
        assertEquals(sslContextId2, exchange2.getProperty(VPExchangeProperties.SSL_CONTEXT_ID, String.class));
        verify(sslContextService, times(1)).getClientSSLContextId(host1);
        verify(sslContextService, times(1)).getClientSSLContextId(host2);
    }

    @Test
    void shouldPropagateExceptionFromSslContextService() {
        Exchange exchange = createExchange();
        String vagvalHost = "failing-host.com";
        exchange.setProperty(VPExchangeProperties.VAGVAL_HOST, vagvalHost);

        RuntimeException expectedException = new RuntimeException("SSL Context Service Error");
        when(sslContextService.getClientSSLContextId(vagvalHost)).thenThrow(expectedException);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> processor.process(exchange));

        assertEquals("SSL Context Service Error", exception.getMessage());
        verify(sslContextService, times(1)).getClientSSLContextId(vagvalHost);
    }

    private Exchange createExchange() {
        return new DefaultExchange(camelContext);
    }
}
