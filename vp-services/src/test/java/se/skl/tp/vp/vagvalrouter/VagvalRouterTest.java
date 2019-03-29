/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis). <http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301  USA
 */
package se.skl.tp.vp.vagvalrouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createAuthorization;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createRouting;

import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.MessageProperties;
import se.skl.tp.vp.util.helper.AddressingHelper;
import se.skl.tp.vp.vagvalagent.VagvalAgentMock;

public class VagvalRouterTest {

  VagvalRouter vagvalRouter;
  VagvalAgentMock vagvalAgent;
  VagvalInput vagvalInput;

  private AddressingHelper helper;

  //JUnit will create a temporary folder before your test, and delete it afterwards
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {

    HsaCache hsaCacheMock = Mockito.mock(HsaCache.class);
    Mockito.when(hsaCacheMock.getParent("VardgivareB")).thenReturn(HsaCache.DEFAUL_ROOTNODE);

    vagvalAgent = new VagvalAgentMock(hsaCacheMock, "#");

    vagvalRouter = new VagvalRouter();
    vagvalRouter.setMessageProperties(MessageProperties.getInstance());
    vagvalRouter.setVagvalAgent(vagvalAgent);


    vagvalInput = new VagvalInput();
    vagvalInput.receiverId = "VardgivareB";
    vagvalInput.senderId = "TP-TEST";
    vagvalInput.rivVersion = "urn:riv:v1";
    vagvalInput.serviceContractNamespace = "{urn:riv13606:v1}RIV";


    helper = new AddressingHelper(vagvalAgent, "VP_INSTANCE_ID");
  }

  @Test
  public void testHappyDaysOneHit() throws Exception {

    vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1",
        "{urn:riv13606:v1}RIV", "VardgivareB"));
    vagvalAgent.getMockAnropsBehorighetsInfo()
        .add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
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
    vagvalAgent.getMockAnropsBehorighetsInfo()
        .add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
            "VardgivareB"));
    String adress = helper.getAddressFromAgent(vagvalInput);
    assertEquals("https://adress1", adress);

  }

  @Test
  public void noContactWithTjanstekatalogenAndNoLocalTakCacheShouldGiveVP008() throws Exception {

    // Remove local cached TAK file
    File localTakCache = new File(folder.getRoot().getPath() + File.separator + ".tk.vagval.localCache");
    File localBehorigheter = new File(folder.getRoot().getPath() + File.separator + ".tk.behorigheter.localCache");
    localTakCache.delete();
    assertFalse("precond: local tak-cache file must not exist", localTakCache.exists());

    // Setup VagvalAgent with no routing or access control added
    VagvalAgentMock vagvalAgentMock = new VagvalAgentMock();
    vagvalAgentMock.setLocalCacheFileName(localBehorigheter.getAbsolutePath());

    AddressingHelper myHelper = new AddressingHelper(vagvalAgentMock, "VP_INSTANCE_ID");

    // Perform the test and ensure that we get a "VP008 no contact..." error message
    try {
      myHelper.getAddressFromAgent(vagvalInput);
      fail("VP008 Exception expected");
    } catch (VpSemanticException e) {
      assertEquals("VP008 No contact with TAK at startup, and no local cache to fallback on, not possible to route call",
          e.getMessage());
    }

  }

  @Test
  public void testManyHitsNoMatchingRivVersion() throws Exception {

    vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress9", "urn:riv:v9",
        "{urn:riv13606:v1}RIV", "VardgivareB"));
    vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress8", "urn:riv:v8",
        "{urn:riv13606:v1}RIV", "VardgivareB"));
    vagvalAgent.getMockAnropsBehorighetsInfo()
        .add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
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
    vagvalAgent.getMockAnropsBehorighetsInfo()
        .add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
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

    vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization("", "", ""));
    try {
      helper.getAddressFromAgent(vagvalInput);
      fail("Exception expected");
    } catch (VpSemanticException e) {
      assertTrue(e.getMessage().startsWith("VP007"));
    }
  }

  @Test
  public void testVirtualiseradTjansteproducentMissing() throws Exception {

    vagvalAgent.getMockVirtualiseringsInfo()
        .add(createRouting("https://adress", "urn:riv:v1", "unknown",
            "VardgivareB"));
    vagvalAgent.getMockAnropsBehorighetsInfo().add(createAuthorization("", "", ""));
    try {
      helper.getAddressFromAgent(vagvalInput);
      fail("Exception expected");
    } catch (VpSemanticException e) {
      assertTrue(e.getMessage().startsWith("VP004"));
    }

  }

  @Test
  public void testReceiverNotSpecified() throws Exception {

    vagvalAgent.getMockVirtualiseringsInfo()
        .add(createRouting("https://adress", "urn:riv:v1", "unknown",
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

    vagvalAgent.getMockVirtualiseringsInfo()
        .add(createRouting("https://adress", "urn:riv:v1", "unknown",
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

    vagvalAgent.getMockVirtualiseringsInfo()
        .add(createRouting("https://adress", "urn:riv:v1", "unknown",
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

    vagvalAgent.getMockVirtualiseringsInfo()
        .add(createRouting("", "urn:riv:v1", "{urn:riv13606:v1}RIV",
            "VardgivareB"));
    vagvalAgent.getMockAnropsBehorighetsInfo()
        .add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
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

    vagvalInput.receiverId = "VardgivareB#VardgivareA";
    vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1",
        "{urn:riv13606:v1}RIV", "VardgivareB"));
    vagvalAgent.getMockAnropsBehorighetsInfo()
        .add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
            "VardgivareB"));
    String adress = helper.getAddressFromAgent(vagvalInput);
    assertEquals("https://adress", adress);

  }

  @Test
  public void testHappyDaysDelimiterHitAfter() throws Exception {

    vagvalInput.receiverId = "VardgivareA#VardgivareB";
    vagvalAgent.getMockVirtualiseringsInfo().add(createRouting("https://adress", "urn:riv:v1",
        "{urn:riv13606:v1}RIV", "VardgivareB"));
    vagvalAgent.getMockAnropsBehorighetsInfo()
        .add(createAuthorization("TP-TEST", "{urn:riv13606:v1}RIV",
            "VardgivareB"));
    String adress = helper.getAddressFromAgent(vagvalInput);
    assertEquals("https://adress", adress);

  }


}
