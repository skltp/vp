package se.skl.tp.vp.vagvalrouter;

import org.mule.tck.FunctionalTestCase;

import se.skl.tjanst1.wsdl.Product;
import se.skl.tp.vp.vagvalagent.VagvalAgent;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer;

public class VpFullServiceTest extends FunctionalTestCase {

	private static final String PRODUCT_ID = "SW123";
	private static final String TJANSTE_ADRESS = "https://localhost:20000/vp/tjanst1";
	
	VagvalInfo vagvalInfo;

	
	public VpFullServiceTest() {
		super();
		setDisposeManagerPerSuite(true);
	}
	
	@Override
	protected String getConfigResources() {
		return "vp-teststubs-and-services-config.xml";
	}
	
	@Override
	protected void doSetUp() throws Exception {
		// NOTE this test user the same certificates for consumer,
		// virtualisation-plattform and producer
		// The certs are located in certs folder and has SERIALNUMBER=tp

		super.doSetUp();
		VagvalAgent vagvalAgent = (VagvalAgent) muleContext.getRegistry().lookupObject(
				"vagvalAgent");
		vagvalAgent.reset();

		// Initialize the vagvalsinfo that is supposed to be in Tjanstekatalogen
		// when the call
		// to the virtual service is made
		// Note certificate serial number is used as sender
		vagvalInfo = (VagvalInfo) muleContext.getRegistry().lookupObject("vagvalInfo");
		vagvalInfo.reset();
	}
	
	public void testHappyDays() throws Exception {

		vagvalInfo.addVagval("vp-test-producer", "tp", "RIVTABP20", "urn:skl:tjanst1:0.1",
				"https://localhost:19000/vardgivare-b/tjanst1");

		Product p = VpFullServiceTestConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS);
		assertEquals(PRODUCT_ID, p.getId());
	}

}
