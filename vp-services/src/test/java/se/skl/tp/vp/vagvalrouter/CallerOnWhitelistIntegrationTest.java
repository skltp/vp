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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import se.skl.tp.vp.util.HttpHeaders;
import se.skl.tp.vp.util.VPUtil;
import se.skltp.domain.subdomain.getproducdetail.v1.Product;

public class CallerOnWhitelistIntegrationTest extends
		CallerOnWhitelistBaseIntegrationTest {

	@Before
	public void doSetUp() throws Exception {
		super.doSetUp();

		assertIpWhiteListPreCondition();
	}

	protected void assertIpWhiteListPreCondition() {
		assertEquals("127.0.0.1", IP_WHITE_LIST);
	}

	@Test
	public void testOkCallerOnWhitelistUsingSenderIdAndVpInstanceId()
			throws Exception {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(HttpHeaders.X_VP_SENDER_ID, "tp");
		properties.put(HttpHeaders.X_VP_INSTANCE_ID, VP_INSTANCE_ID);

		Product p = testConsumer.callGetProductDetail(PRODUCT_ID,
				TJANSTE_ADRESS_HTTP, LOGICAL_ADDRESS, properties);

		assertEquals(PRODUCT_ID, p.getId());
	}

	@Test
	public void testOkCallerOnWhitelistUsingReverseProxyTerminatingTls()
			throws Exception {
		// add headers set by fronting reverse proxy that terminates TLS
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(HttpHeaders.REVERSE_PROXY_HEADER_NAME, clientCertificate);
		properties.put("X-Forwarded-For", "10.10.10.10");
		properties.put(VPUtil.X_MULE_REMOTE_CLIENT_ADDRESS, "127.0.0.1");

		Product p = testConsumer.callGetProductDetail(PRODUCT_ID,
				TJANSTE_ADRESS_HTTP, LOGICAL_ADDRESS, properties);

		assertEquals(PRODUCT_ID, p.getId());
	}
}