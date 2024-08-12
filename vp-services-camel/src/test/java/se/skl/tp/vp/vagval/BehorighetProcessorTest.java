package se.skl.tp.vp.vagval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP002;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP003;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP007;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP008;
import static se.skl.tp.vp.util.takcache.TakCacheMockUtil.createTakCacheLogFailed;
import static se.skl.tp.vp.util.takcache.TakCacheMockUtil.createTakCacheLogOk;
import static se.skl.tp.vp.util.takcache.TestTakDataDefines.*;

import java.net.URL;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.service.TakCacheService;
import se.skltp.takcache.BehorigheterCache;
import se.skltp.takcache.TakCache;
import se.skltp.takcache.VagvalCache;

@CamelSpringBootTest
@SpringBootTest(classes = VagvalTestConfiguration.class)
public class BehorighetProcessorTest  {

    @Autowired
    BehorighetProcessor behorighetProcessor;

    @Autowired
    HsaCache hsaCache;

    @MockBean
    TakCache takCache;

    @Autowired
    TakCacheService takCacheService;
    @MockBean
    BehorigheterCache behorigheterCache;

    @BeforeEach
    public void beforeTest()  {
        URL url = getClass().getClassLoader().getResource("hsacache.xml");
        URL urlHsaRoot = getClass().getClassLoader().getResource("hsacachecomplementary.xml");

        Mockito.when(takCache.refresh()).thenReturn(createTakCacheLogOk());

        hsaCache.init(url.getFile(), urlHsaRoot.getFile());
        takCacheService.refresh();
    }

    @Test
    public void testAuthorizonIsOk() throws Exception {

        Mockito.when(behorigheterCache.isAuthorized(anyString(),anyString(),anyString())).thenReturn(true);

        Exchange ex = createExchangeWithProperties(SENDER_1, NAMNRYMD_1, RECEIVER_1);

        assertFalse(isVpSemanticExceptionThrownWhenProcessed(ex), "testAuthorizonIsOk behorighetProcessor.process should not throw exception");
    }

    @Test
    public void testAuthorizonByClimbingHsaTree() throws Exception {

        Mockito.when(behorigheterCache.isAuthorized(anyString(), anyString(), eq(AUTHORIZED_RECEIVER_IN_HSA_TREE) )).thenReturn(true);
        Mockito.when(behorigheterCache.isAuthorized(anyString(), anyString(), AdditionalMatchers.not(eq(AUTHORIZED_RECEIVER_IN_HSA_TREE)) )).thenReturn(false);

        Exchange ex = createExchangeWithProperties(SENDER_1, NAMNRYMD_1, CHILD_OF_AUTHORIZED_RECEIVER_IN_HSA_TREE);
        assertFalse(isVpSemanticExceptionThrownWhenProcessed(ex), 
        		"testAuthorizonByClimbingHsaTree behorighetProcessor.process should not throw exception");

    }

    @Test
    public void testAuthorizonByClimbingHsaTreeForDisabledNamespace() throws Exception {

        Mockito.when(behorigheterCache.isAuthorized(anyString(), anyString(), eq(AUTHORIZED_RECEIVER_IN_HSA_TREE) )).thenReturn(true);
        Mockito.when(behorigheterCache.isAuthorized(anyString(), anyString(), AdditionalMatchers.not(eq(AUTHORIZED_RECEIVER_IN_HSA_TREE)) )).thenReturn(false);

        try {
            Exchange ex = createExchangeWithProperties(SENDER_1, NAMNRYMD_2, CHILD_OF_AUTHORIZED_RECEIVER_IN_HSA_TREE);
            behorighetProcessor.process(ex);
            fail("Förväntade ett VP007 SemanticException");
        } catch(VpSemanticException vpSemanticException) {
            assertEquals(VP007, vpSemanticException.getErrorCode());
            assertTrue(vpSemanticException.getMessage().contains( "Tjänstekonsumenten saknar behörighet att anropa den logiska adressaten via detta tjänstekontrakt. Kontrollera uppgifterna och vid behov, tillse att det beställs konfiguration i aktuell tjänsteplattform."));
            assertTrue(vpSemanticException.getMessageDetails().contains( "Authorization missing for"));
            assertTrue(vpSemanticException.getMessageDetails().contains( NAMNRYMD_2));
            assertTrue(vpSemanticException.getMessageDetails().contains( SENDER_1));
            assertTrue(vpSemanticException.getMessageDetails().contains( CHILD_OF_AUTHORIZED_RECEIVER_IN_HSA_TREE));
        }
    }

    @Test
    public void testAuthorizonByDefaultRouting() throws Exception {

        Mockito.when(behorigheterCache.isAuthorized(anyString(), anyString(), eq(RECEIVER_2) )).thenReturn(true);
        Mockito.when(behorigheterCache.isAuthorized(anyString(), anyString(),  AdditionalMatchers.not(eq(RECEIVER_2)) )).thenReturn(false);

        Exchange ex = createExchangeWithProperties(SENDER_1, NAMNRYMD_1, RECEIVER_1_DEFAULT_RECEIVER_2);

        assertFalse(isVpSemanticExceptionThrownWhenProcessed(ex), "testAuthorizonByDefaultRouting behorighetProcessor.process should not throw exception");
    }

    private boolean isVpSemanticExceptionThrownWhenProcessed(Exchange ex) throws Exception {
        boolean vpSemanticExceptionThrown = false;
        try {
            behorighetProcessor.process(ex);
        } catch (VpSemanticException e) {
            vpSemanticExceptionThrown = true;
        }
        return vpSemanticExceptionThrown;
    }

    @Test
    public void testNoSenderIdShouldThrowVP002Exception() throws Exception {

        Mockito.when(behorigheterCache.isAuthorized(anyString(),anyString(),anyString())).thenReturn(true);

        try {
            Exchange ex = createExchangeWithProperties(null, NAMNRYMD_1, RECEIVER_1);
            behorighetProcessor.process(ex);
            fail("Förväntade ett VP002 SemanticException");
        }catch(VpSemanticException vpSemanticException){
            assertEquals(VP002, vpSemanticException.getErrorCode());
            assertTrue(vpSemanticException.getMessage().contains("Fel i klientcertifikat. Saknas, är av felaktig typ, eller är felaktigt utformad."));
            assertTrue(vpSemanticException.getMessageDetails().contains("No sender ID (SERIALNUMBER) found in certificate"));
            assertTrue(vpSemanticException.getMessageDetails().contains( NAMNRYMD_1));
            assertTrue(vpSemanticException.getMessageDetails().contains( RECEIVER_1));
        }
    }

    @Test
    public void testNoLogicalAddressShouldThrowVP003Exception() throws Exception {

        Mockito.when(behorigheterCache.isAuthorized(anyString(),anyString(),anyString())).thenReturn(true);

        try {
            Exchange ex = createExchangeWithProperties(SENDER_1, NAMNRYMD_1, null);
            behorighetProcessor.process(ex);
            fail("Förväntade ett VP003 SemanticException");
        }catch(VpSemanticException vpSemanticException){
            assertEquals(VP003, vpSemanticException.getErrorCode());
            assertTrue(vpSemanticException.getMessage().contains( " Logisk adressat (ReceiverId) saknas i RivHeadern i inkommande meddelande."));
            assertTrue(vpSemanticException.getMessageDetails().contains( "No receiverId (logical address) found in message header"));
            assertTrue(vpSemanticException.getMessageDetails().contains( NAMNRYMD_1));
            assertTrue(vpSemanticException.getMessageDetails().contains( SENDER_1));
        }
    }

    @Test
    public void testNotAuthorizedShouldThrowVP007Exception() throws Exception {

        Mockito.when(behorigheterCache.isAuthorized(anyString(),anyString(),anyString())).thenReturn(false);

        try {
            Exchange ex = createExchangeWithProperties(SENDER_1, NAMNRYMD_1, RECEIVER_1);
            behorighetProcessor.process(ex);
            fail("Förväntade ett VP007 SemanticException");
        }catch(VpSemanticException vpSemanticException){
            assertEquals(VP007, vpSemanticException.getErrorCode());
            assertTrue(vpSemanticException.getMessage().contains( "Tjänstekonsumenten saknar behörighet att anropa den logiska adressaten via detta tjänstekontrakt. Kontrollera uppgifterna och vid behov, tillse att det beställs konfiguration i aktuell tjänsteplattform."));
            assertTrue(vpSemanticException.getMessageDetails().contains( "Authorization missing for"));
            assertTrue(vpSemanticException.getMessageDetails().contains( NAMNRYMD_1));
            assertTrue(vpSemanticException.getMessageDetails().contains( SENDER_1));
            assertTrue(vpSemanticException.getMessageDetails().contains( RECEIVER_1));
        }
    }

    @Test
    public void testNotAuthorizedShouldThrowVP008Exception() throws Exception {
        Mockito.when(takCache.refresh()).thenReturn(createTakCacheLogFailed());
        takCacheService.refresh();

        Mockito.when(behorigheterCache.isAuthorized(anyString(),anyString(),anyString())).thenReturn(true);

        try {
            Exchange ex = createExchangeWithProperties(SENDER_1, NAMNRYMD_1, RECEIVER_1);
            behorighetProcessor.process(ex);
            fail("Förväntade ett VP008 SemanticException");
        }catch(VpSemanticException vpSemanticException){
            assertTrue(vpSemanticException.getMessage().contains( "Internt fel i tjänsteplattformen. Ingen kontakt med tjänsteadresseringskatalogen. Informera tjänsteplattformsförvaltningen."));
            assertEquals(vpSemanticException.getErrorCode(), VP008);
        }
    }

    private Exchange createExchangeWithProperties(String senderId, String nameSpace, String receiver) {
        CamelContext ctx = new DefaultCamelContext();
        Exchange ex = new DefaultExchange(ctx);
        ex.setProperty(VPExchangeProperties.SENDER_ID, senderId );
        ex.setProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE, nameSpace);
        ex.setProperty(VPExchangeProperties.RECEIVER_ID, receiver );
        return ex;
    }

}