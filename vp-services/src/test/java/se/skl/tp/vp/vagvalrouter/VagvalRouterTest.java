package se.skl.tp.vp.vagvalrouter;

import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createAuthorization;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createRouting;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleMessage;

import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.helper.AddressingHelper;
import se.skl.tp.vp.vagvalagent.VagvalAgent;
import se.skl.tp.vp.vagvalagent.VagvalAgentMock;

public class VagvalRouterTest extends TestCase {

	VagvalRouter vagvalRouter;
	VagvalAgentMock vagvalAgent;
	VagvalInput vagvalInput;
	String serviceNamespace;
	
	private AddressingHelper helper;

	@Override
	protected void setUp() throws Exception {

		vagvalRouter = new VagvalRouter();
		vagvalAgent = new VagvalAgentMock(new ArrayList<VirtualiseringsInfoType>(), new ArrayList<AnropsBehorighetsInfoType>());
		vagvalRouter.setVagvalAgent(vagvalAgent);
		
		HsaCache hsaCacheMock = Mockito.mock(HsaCache.class);
		Mockito.when(hsaCacheMock.getParent("VardgivareB")).thenReturn(HsaCache.DEFAUL_ROOTNODE);
		vagvalAgent.setHsaCache(hsaCacheMock);

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
		
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		String adress = helper.getAddressFromAgent(vagvalInput);
		assertEquals("https://adress", adress);

	}

	@Test
	public void testManyHits() throws Exception {

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress9", "urn:riv:v9",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress1", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		String adress = helper.getAddressFromAgent(vagvalInput);
		assertEquals("https://adress1", adress);

	}

	@Test
	public void testNoContactTjansteKatalogen() throws Exception {

		// This test can't be run with the VagvalAgentMock since is overrides the getVirtualiseringar() and getBehorigheter() methods.
		// This test however works fine on the original VagValAgent since it doesn't require any mocked TAK-info

		// Setup VagvalAgent with no routing or access control added
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		AddressingHelper myHelper = new AddressingHelper(msg, new VagvalAgent(), Pattern.compile("OU=([^,]+)"), null);
		
		// Remove local cache
		new File(VagvalAgent.TK_LOCAL_CACHE).delete();

		// Perform the test and ensure that we get a "VP008 no contact" error message
		try {
			myHelper.getAddressFromAgent(vagvalInput);
			fail("Exception expected");
		} catch (VpSemanticException e) {
			System.err.println(e.getMessage());
			assertTrue(e.getMessage().startsWith("VP008"));
		}

	}

	@Test
	public void testManyHitsNoMatchingRivVersion() throws Exception {

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress9", "urn:riv:v9",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress8", "urn:riv:v8",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
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

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress9", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress8", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
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

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1",
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

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1", "unknown",
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

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1", "unknown",
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

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1", "unknown",
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

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1", "unknown",
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

		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("", "urn:riv:v1", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
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
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		String adress = helper.getAddressFromAgent(vagvalInput);
		assertEquals("https://adress", adress);

	}

	@Test
	public void testHappyDaysDelimiterHitAfter() throws Exception {

		vagvalAgent.setAddressDelimiter("#");
		vagvalInput.receiverId = "VardgivareA#VardgivareB";
		vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1",
				"{urn:riv13606:v1}RIV", "VardgivareB"));
		vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
				"VardgivareB"));
		String adress = helper.getAddressFromAgent(vagvalInput);
		assertEquals("https://adress", adress);

	}


}
