package se.skl.tp.vp.vagvalrouter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import junit.framework.TestCase;

import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleMessage;

import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoIdType;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoIdType;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;
import se.skl.tp.vp.vagvalagent.VagvalAgent;

public class VagvalRouterTest extends TestCase {

	VagvalRouter vagvalRouter;
	VagvalAgent vagvalAgent;
	VagvalInput vagvalInput;
	String serviceNamespace;
	
	private AddressingHelper helper;

	@Override
	protected void setUp() throws Exception {

		vagvalRouter = new VagvalRouter();
		vagvalAgent = new VagvalAgent();
		vagvalRouter.setVagvalAgent(vagvalAgent);
		vagvalAgent.anropsBehorighetsInfo = new ArrayList<AnropsBehorighetsInfoType>();
		vagvalAgent.virtualiseringsInfo = new ArrayList<VirtualiseringsInfoType>();

		vagvalInput = new VagvalInput();
		vagvalInput.receiverId = "VardgivareB";
		vagvalInput.senderId = "TP-TEST";
		vagvalInput.rivVersion = "urn:riv:v1";
		vagvalInput.serviceNamespace = "{urn:riv13606:v1}RIV";
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		helper = new AddressingHelper(msg, this.vagvalAgent, Pattern.compile("OU=([^,]+)"), null);
	}

	@Test
	public void testHappyDaysOneHit() throws Exception {
		
		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.anropsBehorighetsInfo.add(createAnropsBehorighetsInfoType("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		String adress = helper.getAddressFromAgent(vagvalInput);
		assertEquals("cxf:https://adress", adress);

	}

	@Test
	public void testManyHits() throws Exception {

		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress9", "urn:riv:v9",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress1", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.anropsBehorighetsInfo.add(createAnropsBehorighetsInfoType("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		String adress = helper.getAddressFromAgent(vagvalInput);
		assertEquals("cxf:https://adress1", adress);

	}

	@Test
	public void testNoContactTjansteKatalogen() throws Exception {

		vagvalAgent.anropsBehorighetsInfo = null;
		vagvalAgent.virtualiseringsInfo = null;
		try {
			helper.getAddressFromAgent(vagvalInput);
			fail("Exception expected");
		} catch (VpSemanticException e) {
			assertTrue(e.getMessage().startsWith("VP008"));
		}

	}

	@Test
	public void testManyHitsNoMatchingRivVersion() throws Exception {

		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress9", "urn:riv:v9",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress8", "urn:riv:v8",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.anropsBehorighetsInfo.add(createAnropsBehorighetsInfoType("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));

		try {
			helper.getAddressFromAgent(vagvalInput);
			fail("Exception expected");
		} catch (VpSemanticException e) {
			assertTrue(e.getMessage().startsWith("VP005"));
		}

	}

	@Test
	public void testManyMatchingRivVersion() throws Exception {

		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress9", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress8", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.anropsBehorighetsInfo.add(createAnropsBehorighetsInfoType("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));

		try {
			helper.getAddressFromAgent(vagvalInput);
			fail("Exception expected");
		} catch (VpSemanticException e) {
			assertTrue(e.getMessage().startsWith("VP006"));
		}

	}

	@Test
	public void testBehorighetMissing() throws Exception {

		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		try {
			helper.getAddressFromAgent(vagvalInput);
			fail("Exception expected");
		} catch (VpSemanticException e) {
			assertTrue(e.getMessage().startsWith("VP007"));
		}
	}

	@Test
	public void testVirtualiseradTjansteproducentMissing() throws Exception {

		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress", "urn:riv:v1", "unknown",
				"VardgivareB"));
		try {
			helper.getAddressFromAgent(vagvalInput);
			fail("Exception expected");
		} catch (VpSemanticException e) {
			assertTrue(e.getMessage().startsWith("VP004"));
		}

	}

	@Test
	public void testReceiverNotSpecified() throws Exception {

		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress", "urn:riv:v1", "unknown",
				"VardgivareB"));
		try {
			vagvalInput.receiverId = null;
			helper.getAddressFromAgent(vagvalInput);
			fail("Exception expected");
		} catch (VpSemanticException e) {
			assertTrue(e.getMessage().startsWith("VP003"));
		}

	}

	@Test
	public void testSenderNotSpecified() throws Exception {

		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress", "urn:riv:v1", "unknown",
				"VardgivareB"));
		try {
			vagvalInput.senderId = null;
			helper.getAddressFromAgent(vagvalInput);
			fail("Exception expected");
		} catch (VpSemanticException e) {
			assertTrue(e.getMessage().startsWith("VP002"));
		}

	}

	@Test
	public void testRivVersionNotConfigured() throws Exception {

		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress", "urn:riv:v1", "unknown",
				"VardgivareB"));
		try {
			vagvalInput.rivVersion = null;
			helper.getAddressFromAgent(vagvalInput);
			fail("Exception expected");
		} catch (VpSemanticException e) {
			assertTrue(e.getMessage().startsWith("VP001"));
		}

	}

	@Test
	public void testOneHitEmptyAdress() throws Exception {

		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("", "urn:riv:v1", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		vagvalAgent.anropsBehorighetsInfo.add(createAnropsBehorighetsInfoType("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		try {
			helper.getAddressFromAgent(vagvalInput);
			fail("Exception expected");
		} catch (VpSemanticException e) {
			assertTrue(e.getMessage(), e.getMessage().startsWith("VP010"));
		}

	}

	@Test
	public void testHappyDaysDelimiterHitBefore() throws Exception {

		vagvalAgent.setAddressDelimiter("#");
		vagvalInput.receiverId = "VardgivareB#VardgivareA";
		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.anropsBehorighetsInfo.add(createAnropsBehorighetsInfoType("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		String adress = helper.getAddressFromAgent(vagvalInput);
		assertEquals("cxf:https://adress", adress);

	}

	@Test
	public void testHappyDaysDelimiterHitAfter() throws Exception {

		vagvalAgent.setAddressDelimiter("#");
		vagvalInput.receiverId = "VardgivareA#VardgivareB";
		vagvalAgent.virtualiseringsInfo.add(createVirtualiseringsInfoType("https://adress", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.anropsBehorighetsInfo.add(createAnropsBehorighetsInfoType("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		String adress = helper.getAddressFromAgent(vagvalInput);
		assertEquals("cxf:https://adress", adress);

	}

	private VirtualiseringsInfoType createVirtualiseringsInfoType(String adress, String rivVersion, String namnrymnd,
			String receiver) throws Exception {

		XMLGregorianCalendar fromTidpunkt = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		Duration anHourAgo = DatatypeFactory.newInstance().newDuration(false, 0, 0, 0, 1, 0, 0);
		fromTidpunkt.add(anHourAgo);

		XMLGregorianCalendar tomTidpunkt = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		Duration tenYearsDuration = DatatypeFactory.newInstance().newDurationYearMonth(true, new BigInteger("10"),
				new BigInteger("2"));
		tomTidpunkt.add(tenYearsDuration);

		VirtualiseringsInfoType vi = new VirtualiseringsInfoType();
		vi.setAdress(adress);
		vi.setFromTidpunkt(fromTidpunkt);
		vi.setTomTidpunkt(tomTidpunkt);
		vi.setReceiverId(receiver);
		vi.setRivProfil(rivVersion);
		VirtualiseringsInfoIdType viId = new VirtualiseringsInfoIdType();
		viId.setValue(String.valueOf(1));
		vi.setVirtualiseringsInfoId(viId);
		vi.setTjansteKontrakt(namnrymnd);
		return vi;

	}

	private AnropsBehorighetsInfoType createAnropsBehorighetsInfoType(String sender, String namnrymd, String receiver)
			throws Exception {

		Duration tenYearsDuration = DatatypeFactory.newInstance().newDurationYearMonth(true, new BigInteger("10"),
				new BigInteger("2"));
		Duration anHourAgo = DatatypeFactory.newInstance().newDuration(false, 0, 0, 0, 1, 0, 0);

		XMLGregorianCalendar fromTidpunkt = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		fromTidpunkt.add(anHourAgo);

		XMLGregorianCalendar tomTidpunkt = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		tomTidpunkt.add(tenYearsDuration);

		AnropsBehorighetsInfoIdType aboId = new AnropsBehorighetsInfoIdType();
		aboId.setValue(String.valueOf(1));
		AnropsBehorighetsInfoType abo = new AnropsBehorighetsInfoType();
		abo.setAnropsBehorighetsInfoId(aboId);
		abo.setFromTidpunkt(fromTidpunkt);
		abo.setTomTidpunkt(tomTidpunkt);
		abo.setReceiverId(receiver);
		abo.setSenderId(sender);
		abo.setTjansteKontrakt(namnrymd);
		return abo;
	}

}
