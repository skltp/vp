package se.skl.tp.vp.vagvalagent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigInteger;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Before;
import org.junit.Test;
import org.mule.tck.junit4.FunctionalTestCase;

import se.skl.tp.vagval.wsdl.v1.ResetVagvalCacheRequest;
import se.skl.tp.vagval.wsdl.v1.ResetVagvalCacheResponse;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalRequest;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalResponse;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;

public class VagvalAgentIntegrationTest extends FunctionalTestCase {

	private VagvalAgent vagvalAgent;

	private static final String vardgivareB = "SE0000000003-1234";
	private static final String vardenhetA = "SE0000000001-1234";

	private static final String konsumentA = "konsumentA";

	@Override
	protected String getConfigResources() {
		return "teststub-services/VagvalAgentTest-teststub-service.xml";
	}

	@Before
	public void doSetUp() throws Exception {
		super.doSetUp();
		vagvalAgent = (VagvalAgent) muleContext.getRegistry().lookupObject("vagvalAgent");
		vagvalAgent.resetVagvalCache(new ResetVagvalCacheRequest());
		
		resetVagvalInput();

		addVagvalInput(vardgivareB, "rivtabp20", konsumentA,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1");
		addVagvalInput(vardgivareB, "rivtabp21", konsumentA,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1");
	}

	private void resetVagvalInput() {
		SokVagvalsInfoMockInput vagvalInputs = (SokVagvalsInfoMockInput) muleContext.getRegistry().lookupObject(
				"vagvalInputs");
		vagvalInputs.reset();
	}

	private void addVagvalInput(String receiverId, String rivVersion, String senderId, String serviceNameSpace) {

		SokVagvalsInfoMockInput vagvalInputs = (SokVagvalsInfoMockInput) muleContext.getRegistry().lookupObject(
				"vagvalInputs");

		VagvalMockInputRecord vagvalInput = new VagvalMockInputRecord();
		vagvalInput.receiverId = receiverId;
		vagvalInput.rivVersion = rivVersion;
		vagvalInput.senderId = senderId;
		vagvalInput.serviceNamespace = serviceNameSpace;

		vagvalInputs.getVagvalInputs().add(vagvalInput);

	}

	@Test
	public void testGiltigaVagval() throws Exception {

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB, null,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals(2, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalDelimiter() throws Exception {

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB + "#"
				+ vardenhetA, null, "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals(2, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalObehorig() throws Exception {

		try {
			vagvalAgent.visaVagval(createVisaVagvalRequest("XXX", vardgivareB, null,
					"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
			fail("Exception expected");
		} catch (VpSemanticException e) {
		}
	}

	@Test
	public void testGiltigaVagvalIngetKontrakt() throws Exception {

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB, null,
				"XXX"));
		assertEquals(0, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalIngenMottagare() throws Exception {

		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, "XXX", null,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals(0, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void testGiltigaVagvalOgiltigTid() throws Exception {

		Duration twentyYearsDuration = DatatypeFactory.newInstance().newDurationYearMonth(true, new BigInteger("20"),
				new BigInteger("2"));
		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB,
				twentyYearsDuration, "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals(0, vvResponse.getVirtualiseringsInfo().size());
	}

	@Test
	public void resetCacheUpdatesVagvalAgent() throws Exception {
		VisaVagvalResponse vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB, null,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
		assertEquals(2, vvResponse.getVirtualiseringsInfo().size());

		String receiverId = vardgivareB;
		String rivVersion = "rivtabp20";
		String senderId = konsumentA;
		String serviceNameSpace = "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1";

		addVagvalInput(receiverId, rivVersion, senderId, serviceNameSpace);

		ResetVagvalCacheResponse rvcResponse = vagvalAgent.resetVagvalCache(createResetVagvalCacheRequest());
		assertTrue(rvcResponse.isResetResult());

		vvResponse = vagvalAgent.visaVagval(createVisaVagvalRequest(konsumentA, vardgivareB, null,
				"urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1"));
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
