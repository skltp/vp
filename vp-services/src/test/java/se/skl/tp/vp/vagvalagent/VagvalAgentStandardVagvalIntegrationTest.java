package se.skl.tp.vp.vagvalagent;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.takcache.RoutingInfo;

public class VagvalAgentStandardVagvalIntegrationTest extends AbstractTestCase {

  public static final String RIV_CONTRACT_GETSUBJECTOFCARESCHEDULE = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";
  public static final String RIV_CONTRACT_GETPRODUCTDETAIL = "urn:riv:domain:subdomain:GetProductDetailResponder:1";
  public static final String RIVTABP20 = "rivtabp20";
  public static final String RIVTABP21 = "rivtabp21";
  private static final String DEFAULT_ADDRESS = "https://default/address.htm";
  private static final String DIRECT_ADDRESS = "https://specific/address.htm";
  private static final String HSA_ROOT_ADDRESS = "https://hsa/address.htm";
  private VagvalAgent vagvalAgent;

  private static final String RECEIVER_A = "SE0000000001-1234";
  private static final String RECEIVER_HSA_ROOT = "SE0000000003-1234";
  private static final String RECEIVER_C = "SE0000000054-1234";
  private static final String RECEIVER_NO_DIRECT_VAGVAL = "SE0000000055-1234";
  private static final String RECEIVER_DEFAULT = "*";

  private static final String CONSUMENT_A = "konsumentA";

  @Rule
  public ExpectedException thrown = ExpectedException.none();


  static SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();

  @Override
  protected String[] getConfigFiles() {
    return new String[]{"soitoolkit-mule-jms-connector-activemq-embedded.xml",
        "vp-common.xml",
        "services/VagvalRouter-service.xml",
        "vp-teststubs-and-services-config.xml"};
  }

  private void setupTjanstekatalogen() throws Exception {
    List<VagvalMockInputRecord> vagvalInputs = new ArrayList<>();

    vagvalInputs.add(createVagvalBehorighetRecord(RECEIVER_HSA_ROOT, CONSUMENT_A, RIV_CONTRACT_GETSUBJECTOFCARESCHEDULE, HSA_ROOT_ADDRESS));
    vagvalInputs.add(createVagvalBehorighetRecord(RECEIVER_C, CONSUMENT_A, RIV_CONTRACT_GETSUBJECTOFCARESCHEDULE, DIRECT_ADDRESS));
    vagvalInputs.add(createVagvalBehorighetRecord(RECEIVER_DEFAULT, CONSUMENT_A, RIV_CONTRACT_GETSUBJECTOFCARESCHEDULE, DEFAULT_ADDRESS));
    vagvalInputs.add(createVagvalRecord(RECEIVER_DEFAULT, RIV_CONTRACT_GETPRODUCTDETAIL, DEFAULT_ADDRESS));
    vagvalInputs.add(createBehorighetRecord(RECEIVER_C, CONSUMENT_A, RIV_CONTRACT_GETPRODUCTDETAIL));
    svimi.setVagvalInputs(vagvalInputs);
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    setupTjanstekatalogen();
  }

  @Override
  public void doSetUp() throws Exception {
    vagvalAgent = (VagvalAgent) muleContext.getRegistry().lookupObject("vagvalAgent");
  }

  @Test
  public void testOnlyDefaultVagvalShouldGiveDefaultRouting() throws Exception {

    List<RoutingInfo> routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(CONSUMENT_A, RECEIVER_NO_DIRECT_VAGVAL, RIV_CONTRACT_GETSUBJECTOFCARESCHEDULE));
    assertEquals(1, routingInfos.size());
    assertEquals(DEFAULT_ADDRESS, routingInfos.get(0).getAddress());
  }

  @Test
  public void testBothDefaultVagvalAndDirektVagvalShouldGiveDirektRouting() throws Exception {

    List<RoutingInfo> routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(CONSUMENT_A, RECEIVER_C,
        RIV_CONTRACT_GETSUBJECTOFCARESCHEDULE));
    assertEquals(1, routingInfos.size());
    assertEquals(DIRECT_ADDRESS, routingInfos.get(0).getAddress());
  }

  @Test
  public void testDefaultVagvalButNoDefaultBehorighetShouldGiveVP007() throws Exception {

    thrown.expect(VpSemanticException.class);
    thrown.expectMessage("VP007");
    vagvalAgent.visaVagval(createVisaVagvalRequest(CONSUMENT_A, RECEIVER_NO_DIRECT_VAGVAL, RIV_CONTRACT_GETPRODUCTDETAIL));
  }

  @Test
  public void testDefaultVagvalAndDirectBehorighetShouldGiveDefaultRouting() throws Exception {

    List<RoutingInfo> routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(CONSUMENT_A, RECEIVER_C, RIV_CONTRACT_GETPRODUCTDETAIL));
    assertEquals(1, routingInfos.size());
    assertEquals(DEFAULT_ADDRESS, routingInfos.get(0).getAddress());
  }

  @Test
  public void testHsaRoutingShouldBeDoneBeforeStandardVagval() throws Exception {

    List<RoutingInfo> routingInfos = vagvalAgent.visaVagval(createVisaVagvalRequest(CONSUMENT_A, RECEIVER_A, RIV_CONTRACT_GETSUBJECTOFCARESCHEDULE));
    assertEquals(1, routingInfos.size());
    assertEquals(HSA_ROOT_ADDRESS, routingInfos.get(0).getAddress());
  }

  private VisaVagvalRequest createVisaVagvalRequest(String senderId, String receiverId,
      String tjansteGranssnitt) {

    VisaVagvalRequest vvR = new VisaVagvalRequest();
    vvR.setSenderId(senderId);
    vvR.setReceiverId(receiverId);
    vvR.setTjanstegranssnitt(tjansteGranssnitt);
    return vvR;
  }

  private static VagvalMockInputRecord createVagvalBehorighetRecord(String receiverId,String senderId, String
      serviceNameSpace, String adress) {

    VagvalMockInputRecord vagvalInput = new VagvalMockInputRecord();
    vagvalInput.receiverId = receiverId;
    vagvalInput.rivVersion = RIVTABP21;
    vagvalInput.senderId = senderId;
    vagvalInput.serviceContractNamespace = serviceNameSpace;
    vagvalInput.adress = adress;
    return vagvalInput;
  }

  private static VagvalMockInputRecord createBehorighetRecord(String receiverId,String senderId, String
      serviceNameSpace) {
    VagvalMockInputRecord vagvalInput = createVagvalBehorighetRecord(receiverId, senderId, serviceNameSpace, null);
    vagvalInput.addVagval=false;
    return vagvalInput;
  }

  private static VagvalMockInputRecord createVagvalRecord(String receiverId, String serviceNameSpace, String adress) {
    VagvalMockInputRecord vagvalInput = createVagvalBehorighetRecord(receiverId, null, serviceNameSpace, adress);
    vagvalInput.addBehorighet=false;
    return vagvalInput;
  }

}
