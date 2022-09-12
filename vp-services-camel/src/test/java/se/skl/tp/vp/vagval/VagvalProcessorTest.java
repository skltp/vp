package se.skl.tp.vp.vagval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP003;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP004;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP006;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP008;
import static se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum.VP010;
import static se.skl.tp.vp.util.soaprequests.RoutingInfoUtil.createRoutingInfo;
import static se.skl.tp.vp.util.takcache.TakCacheMockUtil.createTakCacheLogFailed;
import static se.skl.tp.vp.util.takcache.TakCacheMockUtil.createTakCacheLogOk;
import static se.skl.tp.vp.util.takcache.TestTakDataDefines.ADDRESS_1;
import static se.skl.tp.vp.util.takcache.TestTakDataDefines.NAMNRYMD_1;
import static se.skl.tp.vp.util.takcache.TestTakDataDefines.RECEIVER_1;
import static se.skl.tp.vp.util.takcache.TestTakDataDefines.RIV20;
import static se.skl.tp.vp.util.takcache.TestTakDataDefines.RIV21;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.service.TakCacheService;
import se.skltp.takcache.*;

@CamelSpringBootTest
@SpringBootTest(classes = VagvalTestConfiguration.class)
public class VagvalProcessorTest {

  @Autowired
  VagvalProcessor vagvalProcessor;
  @Autowired
  HsaCache hsaCache;
  @Autowired
  TakCacheService takCacheService;
  @MockBean
  VagvalCache vagvalCache;
  @MockBean
  TakCache takCache;

  @BeforeEach
  public void beforeTest() {
    URL url = getClass().getClassLoader().getResource("hsacache.xml");
    URL urlHsaRoot = getClass().getClassLoader().getResource("hsacachecomplementary.xml");
    hsaCache.init(url.getFile(), urlHsaRoot.getFile());

    Mockito.when(takCache.refresh()).thenReturn(createTakCacheLogOk());
    takCacheService.refresh();
  }

  @Test
  public void testVagvalFound() throws Exception
  {
    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo(ADDRESS_1, RIV20));
    Mockito.when(vagvalCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_1)).thenReturn(list);

    Exchange ex = createExchangeWithProperties(NAMNRYMD_1, RECEIVER_1);
    vagvalProcessor.process(ex);

    assertEquals(ADDRESS_1, ex.getProperty(VPExchangeProperties.VAGVAL));
    assertEquals(RIV20, ex.getProperty(VPExchangeProperties.RIV_VERSION_OUT));

  }

  //@Test
  public void testVagvalDefaultHttpsPort() throws Exception {

    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo("https://tjp.nordicmedtest.se/skaulo/vp/clinicalprocess/activityprescription/actoutcome/GetMedicationHistory/2/rivtabp21", RIV20));
    Mockito.when(vagvalCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_1)).thenReturn(list);

    Exchange ex = createExchangeWithProperties(NAMNRYMD_1, RECEIVER_1);
    vagvalProcessor.process(ex);

    assertEquals("tjp.nordicmedtest.se:443", ex.getProperty(VPExchangeProperties.VAGVAL_HOST));
    assertEquals(RIV20, ex.getProperty(VPExchangeProperties.RIV_VERSION_OUT));

  }

  //@Test
  public void testVagvalDefaultHttpPort() throws Exception {

    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo("http://tjp.nordicmedtest.se/skaulo/vp/clinicalprocess/activityprescription/actoutcome/GetMedicationHistory/2/rivtabp21", RIV20));
    Mockito.when(vagvalCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_1)).thenReturn(list);

    Exchange ex = createExchangeWithProperties(NAMNRYMD_1, RECEIVER_1);
    vagvalProcessor.process(ex);

    assertEquals("tjp.nordicmedtest.se:8080", ex.getProperty(VPExchangeProperties.VAGVAL_HOST));
  }

  @Test
  public void testNoLogicaAddressInRequestShouldThrowVP003Exception() throws Exception {

    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo(ADDRESS_1, RIV20));
    Mockito.when(vagvalCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_1)).thenReturn(list);

    try {
      Exchange ex = createExchangeWithProperties(NAMNRYMD_1, null);
      vagvalProcessor.process(ex);
      fail("Förväntade ett VP003 SemanticException");
    } catch (VpSemanticException vpSemanticException) {
      assertEquals(VP003, vpSemanticException.getErrorCode());
      assertTrue(vpSemanticException.getMessage().contains("Logisk adressat (ReceiverId) saknas i RivHeadern i inkommande meddelande"));
      assertTrue(vpSemanticException.getMessageDetails().contains("No receiverId (logical address) found in message header"));
    }
  }

  @Test
  public void testNoVagvalFoundShouldThrowVP004Exception() throws Exception {

    Mockito.when(vagvalCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_1)).thenReturn(Collections.emptyList());

    try {
      Exchange ex = createExchangeWithProperties(NAMNRYMD_1, RECEIVER_1);
      vagvalProcessor.process(ex);
      fail("Förväntade ett VP004 SemanticException");
    } catch (VpSemanticException vpSemanticException) {
      assertEquals(VP004, vpSemanticException.getErrorCode());
      assertTrue(vpSemanticException.getMessage().contains("Det finns inget vägval i tjänsteadresseringskatalogen som matchar anropets logiska adressat (ReceiverId) och tjänstekontrakt. Kontrollera uppgifterna i anropet och vid behov, beställ konfigurering i aktuell tjänsteplattform."));
      assertTrue(vpSemanticException.getMessageDetails().contains("No receiverId (logical address) found for"));
      assertTrue(vpSemanticException.getMessageDetails().contains(NAMNRYMD_1));
      assertTrue(vpSemanticException.getMessageDetails().contains(RECEIVER_1));
    }
  }

  @Test
  public void testTooManyVagvalFoundShouldThrowVP006Exception() throws Exception {

    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo(ADDRESS_1, RIV20));
    list.add(createRoutingInfo(ADDRESS_1, RIV21));
    Mockito.when(vagvalCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_1)).thenReturn(list);

    try {
      Exchange ex = createExchangeWithProperties(NAMNRYMD_1, RECEIVER_1);
      vagvalProcessor.process(ex);
      fail("Förväntade ett VP006 SemanticException");
    } catch (VpSemanticException vpSemanticException) {
      assertEquals(VP006, vpSemanticException.getErrorCode());
      assertTrue(vpSemanticException.getMessage().contains("Internt fel i tjänsteplattformen. Det finns fler än en tjänsteproducent definierad i tjänsteadresseringskatalogen som matchar logisk adressat (ReceiverId), tjänstekontrakt och dagens datum. Tyder på felkonfiguration. Rapportera felet till tjänsteplattformsförvaltningen."));
      assertTrue(vpSemanticException.getMessageDetails()
          .contains("More than one receiverId (logical address) with matching Riv-version found for"));
      assertTrue(vpSemanticException.getMessageDetails().contains(NAMNRYMD_1));
      assertTrue(vpSemanticException.getMessageDetails().contains(RECEIVER_1));
    }
  }

  @Test
  public void failedInitTakCacheShouldThrowVP008Exception() throws Exception {
    Mockito.when(takCache.refresh()).thenReturn(createTakCacheLogFailed());
    takCacheService.refresh();

    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo(ADDRESS_1, RIV20));
    Mockito.when(vagvalCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_1)).thenReturn(list);

    try {
      Exchange ex = createExchangeWithProperties(NAMNRYMD_1, RECEIVER_1);
      vagvalProcessor.process(ex);
      fail("Förväntade ett VP008 SemanticException");
    } catch (VpSemanticException vpSemanticException) {
      assertEquals(VP008, vpSemanticException.getErrorCode());
      assertTrue(vpSemanticException.getMessage().contains("Internt fel i tjänsteplattformen. Ingen kontakt med tjänsteadresseringskatalogen. Informera tjänsteplattformsförvaltningen."));
    }
  }

  @Test
  public void testIfFoundVagvalAddressIsEmptyItShouldThrowVP010Exception() throws Exception {

    List<RoutingInfo> list = new ArrayList<>();
    list.add(createRoutingInfo("", RIV20));
    Mockito.when(vagvalCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_1)).thenReturn(list);

    try {
      Exchange ex = createExchangeWithProperties(NAMNRYMD_1, RECEIVER_1);
      vagvalProcessor.process(ex);
      fail("Förväntade ett VP010 SemanticException");
    } catch (VpSemanticException vpSemanticException) {
      assertEquals(VP010, vpSemanticException.getErrorCode());
      assertTrue(vpSemanticException.getMessage().contains("Internt fel i tjänsteplattformen. URL saknas för tjänsteproducenten i tjänsteplattformens tjänsteadresseringskatalog."));
      assertTrue(vpSemanticException.getMessageDetails().contains("Physical Address field is empty in Service Producer for"));
      assertTrue(vpSemanticException.getMessageDetails().contains(NAMNRYMD_1));
      assertTrue(vpSemanticException.getMessageDetails().contains(RECEIVER_1));
    }
  }

  private Exchange createExchangeWithProperties(String nameSpace, String receiver) {
    CamelContext ctx = new DefaultCamelContext();
    Exchange ex = new DefaultExchange(ctx);
    ex.setProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE, nameSpace);
    ex.setProperty(VPExchangeProperties.RECEIVER_ID, receiver);
    return ex;
  }


}
