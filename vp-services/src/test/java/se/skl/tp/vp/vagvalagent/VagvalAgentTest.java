package se.skl.tp.vp.vagvalagent;

import java.math.BigInteger;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.tck.FunctionalTestCase;

import se.skl.tp.vagval.wsdl.v1.ResetVagvalCacheRequest;
import se.skl.tp.vagval.wsdl.v1.ResetVagvalCacheResponse;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalRequest;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalResponse;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.vagvalrouter.VagvalInput;

public class VagvalAgentTest extends FunctionalTestCase {

	private VagvalAgent vagvalAgent;

	@Override
	protected String getConfigResources() {
		return "teststub-services/VagvalAgentTest-teststub-service.xml";
	}

	@BeforeClass
	public void beforeClass() {
		setDisposeManagerPerSuite(true);
	}

	@Before
	public void doSetUp() throws Exception {
		super.doSetUp();
		vagvalAgent = (VagvalAgent) muleContext.getRegistry().lookupObject("vagvalAgent");
		vagvalAgent.reset();

		resetVagvalInput();
		
		addVagvalInput("VardgivareB", "urn:riv:v1", "tppoc-vardgivare_A-tjanst1",
				"{urn:riv13606:v1}RIV13606REQUEST_EHR_EXTRACT_PortType");

		addVagvalInput("VardgivareB", "urn:riv:v2", "tppoc-vardgivare_A-tjanst1",
				"{urn:riv13606:v1}RIV13606REQUEST_EHR_EXTRACT_PortType");
	}

	private void resetVagvalInput() {
		SokVagvalsInfoMockInput vagvalInputs = (SokVagvalsInfoMockInput) muleContext.getRegistry().lookupObject("vagvalInputs");
		vagvalInputs.reset();
	}

	private void addVagvalInput(String receiverId, String rivVersion, String senderId, String serviceNameSpace) {

		SokVagvalsInfoMockInput vagvalInputs = (SokVagvalsInfoMockInput) muleContext.getRegistry().lookupObject("vagvalInputs");

		VagvalMockInputRecord vagvalInput = new VagvalMockInputRecord();
		vagvalInput.receiverId = receiverId;
		vagvalInput.rivVersion = rivVersion;
		vagvalInput.senderId = senderId;
		vagvalInput.serviceNamespace = serviceNameSpace;

		vagvalInputs.getVagvalInputs().add(vagvalInput);

	}

	@Test
	public void testGiltigaVagval() throws Exception {

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest("tppoc-vardgivare_A-tjanst1",
				"VardgivareB", null, "{urn:riv13606:v1}RIV13606REQUEST_EHR_EXTRACT_PortType"));
		assertEquals(2, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalDelimiter() throws Exception {

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest("tppoc-vardgivare_A-tjanst1",
				"VardgivareB#VardgivareA", null, "{urn:riv13606:v1}RIV13606REQUEST_EHR_EXTRACT_PortType"));
		assertEquals(2, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalObehorig() throws Exception {

		try {
			vagvalAgent.visaVagval(createVisaVagvalRequest("XXX", "VardgivareB", null,
					"{urn:riv13606:v1}RIV13606REQUEST_EHR_EXTRACT_PortType"));
			fail("Exception expected");
		} catch (VpSemanticException e) {
		}
	}

	@Test
	public void testGiltigaVagvalIngetKontrakt() throws Exception {

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest("tppoc-vardgivare_A-tjanst1",
				"VardgivareB", null, "XXX"));
		assertEquals(0, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalIngenMottagare() throws Exception {

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest("tppoc-vardgivare_A-tjanst1",
				"XXX", null, "{urn:riv13606:v1}RIV13606REQUEST_EHR_EXTRACT_PortType"));
		assertEquals(0, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalOgiltigTid() throws Exception {

		Duration twentyYearsDuration = DatatypeFactory.newInstance().newDurationYearMonth(true, new BigInteger("20"),
				new BigInteger("2"));
		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest("tppoc-vardgivare_A-tjanst1",
				"VardgivareB", twentyYearsDuration, "{urn:riv13606:v1}RIV13606REQUEST_EHR_EXTRACT_PortType"));
		assertEquals(0, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalAfterCacheUpdate() throws Exception {
		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest("tppoc-vardgivare_A-tjanst1",
				"VardgivareB", null, "{urn:riv13606:v1}RIV13606REQUEST_EHR_EXTRACT_PortType"));
		assertEquals(2, vvResponse.getVirtualiseringsInfo().size());

		String receiverId = "VardgivareB";
		String rivVersion = "urn:riv:v3";
		String senderId = "tppoc-vardgivare_A-tjanst1";
		String serviceNameSpace = "{urn:riv13606:v1}RIV13606REQUEST_EHR_EXTRACT_PortType";

		addVagvalInput(receiverId, rivVersion, senderId, serviceNameSpace);

		ResetVagvalCacheResponse rvcResponse = vagvalAgent.resetVagvalCache(createResetVagvalCacheRequest());
		assertTrue(rvcResponse.isResetResult());

		vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest("tppoc-vardgivare_A-tjanst1", "VardgivareB", null,
				"{urn:riv13606:v1}RIV13606REQUEST_EHR_EXTRACT_PortType"));
		assertEquals(3, vvResponse.getVirtualiseringsInfo().size());
	}

	private VisaVagvalRequest createVisaVagvalRequest(String senderId, String receiverId, Duration addToTidPunkt,
			String tjansteGranssnitt) {

		VisaVagvalRequest vvR = new VisaVagvalRequest();
		vvR.setSenderId(senderId);
		vvR.setReceiverId(receiverId);
		vvR.setTjanstegranssnitt(tjansteGranssnitt);
		XMLGregorianCalendar tidPunkt = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		if (addToTidPunkt != null) {
			tidPunkt.add(addToTidPunkt);
		}
		vvR.setTidpunkt(tidPunkt);
		return vvR;
	}

	private ResetVagvalCacheRequest createResetVagvalCacheRequest() {
		ResetVagvalCacheRequest rvcR = new ResetVagvalCacheRequest();
		return rvcR;
	}

}
