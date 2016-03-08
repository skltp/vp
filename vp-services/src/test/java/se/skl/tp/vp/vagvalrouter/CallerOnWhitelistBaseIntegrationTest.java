/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.vp.vagvalrouter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;

public class CallerOnWhitelistBaseIntegrationTest extends AbstractTestCase {

	protected static final int CLIENT_TIMEOUT_MS = 60000;
	protected static final String PRODUCT_ID = "SW123";
	protected static final String TJANSTE_ADRESS_HTTP = "http://localhost:8080/vp/tjanst1";
	protected static final String LOGICAL_ADDRESS = "vp-test-producer";

	protected static final RecursiveResourceBundle rb = new RecursiveResourceBundle(
			"vp-config", "vp-config-override");
	protected static final String VP_INSTANCE_ID = rb
			.getString("VP_INSTANCE_ID");
	protected static final String IP_WHITE_LIST = rb.getString("IP_WHITE_LIST");

	protected static VpFullServiceTestConsumer_MuleClient testConsumer = null;
	protected static String clientCertificate;

	public CallerOnWhitelistBaseIntegrationTest() {
		super();

		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
		setDisposeContextPerClass(true);

		SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS,
				"https://localhost:19000/vardgivare-b/tjanst1"));
		svimi.setVagvalInputs(vagvalInputs);
	}

	private VagvalMockInputRecord createVagvalRecord(String receiverId,
			String adress) {
		VagvalMockInputRecord vi_TP = new VagvalMockInputRecord();
		vi_TP.receiverId = receiverId;
		vi_TP.senderId = "tp";
		vi_TP.rivVersion = "RIVTABP20";
		vi_TP.serviceContractNamespace = "urn:riv:domain:subdomain:GetProductDetailResponder:1";
		vi_TP.adress = adress;
		return vi_TP;
	}

	@Override
	protected String getConfigResources() {
		return "soitoolkit-mule-jms-connector-activemq-embedded.xml,"
				+ "vp-common.xml," + "services/VagvalRouter-service.xml,"
				+ "vp-teststubs-and-services-config.xml";
	}

	@Before
	public void doSetUp() throws Exception {
		super.doSetUp();

		if (testConsumer == null) {
			testConsumer = new VpFullServiceTestConsumer_MuleClient(
					muleContext, "VPInsecureConnector", CLIENT_TIMEOUT_MS);
		}
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		clientCertificate = readClientCertificateFromFile();
	}

	private static String readClientCertificateFromFile() throws IOException {
		// read certificate to be set as HTTP header
		String clientCert = FileUtils.readFileToString(new File(
				"src/test/resources/certs/cert_ou_is_tp.pem"), "US-ASCII");
		clientCert = clientCert.replace('\n', ' ');
		return clientCert;
	}

}