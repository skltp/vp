package se.skl.tp.vp.vagvalrouter;

import java.util.ArrayList;
import java.util.List;

import org.mule.tck.FunctionalTestCase;

import se.skl.tjanst1.wsdl.Product;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;

public class VpFullServiceTest extends FunctionalTestCase {

	private static final String PRODUCT_ID = "SW123";
	private static final String TJANSTE_ADRESS = "https://localhost:20000/vp/tjanst1";
	
	private static VpFullServiceTestConsumer_MuleClient testConsumer = null;

	VagvalInfo vagvalInfo;

	
	public VpFullServiceTest() {
		super();
		setDisposeManagerPerSuite(true);
		
		SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		VagvalMockInputRecord vi = new VagvalMockInputRecord();
		vi.receiverId = "vp-test-producer";
		vi.senderId = "tp";
		vi.rivVersion = "RIVTABP20";
		vi.serviceNamespace = "urn:skl:tjanst1:rivtabp20";
		vi.adress = "https://localhost:19000/vardgivare-b/tjanst1";
		vagvalInputs.add(vi);
		svimi.setVagvalInputs(vagvalInputs);
		
	}
	
	@Override
	protected String getConfigResources() {
		return 
			"soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
			"vp-common.xml," +
			"services/VagvalRouter-service.xml," +
			"vp-teststubs-and-services-config.xml";
	}
	
	@Override
	protected void doSetUp() throws Exception {
		if (testConsumer == null) {
			testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPConsumerConnector");
		}
	}
	
	public void testHappyDays() throws Exception {

		Product p = testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS);
		assertEquals(PRODUCT_ID, p.getId());
	}
}