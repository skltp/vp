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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import se.skl.tp.vp.util.HttpHeaders;

public class CallerNotOnWhitelistIntegrationTest extends
		CallerOnWhitelistBaseIntegrationTest {

	@BeforeClass
	public static void setupBeforeClass() {
		// override value in property file before injecting into spring context
		System.setProperty("IP_WHITE_LIST", "1.1.1.1");
	}

	@AfterClass
	public static void tearDownAfterClass() {
		// restore
		System.clearProperty("IP_WHITE_LIST");
	}

	/**
	 * Verify that when caller is using http and correct values for http headers
	 * x-vp-sender-id and x-vp-instance-id, a check is done against ip
	 * whitelist. In this case ip whitelist does not contain 127.0.0.1.
	 */
	@Test
	public void testVP011IsThrownWhenCallerIsNotOnWhitelistUsingHeaderX_VP_SENDER_ID()
			throws Exception {
		/*
		 * Provide a valid vp instance id and x-vp-sender-id to trigger a check
		 * on the ip whitelist.
		 */
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(HttpHeaders.X_VP_SENDER_ID, "tp");
		properties.put(HttpHeaders.X_VP_INSTANCE_ID, VP_INSTANCE_ID);

		try {
			testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_HTTP,
					LOGICAL_ADDRESS, properties);
			fail("Expected error here!");
		} catch (Exception ex) {
			assertTrue(ex
					.getMessage()
					.contains(
							"VP011 Caller was not on the white list of accepted IP-addresses. IP-address: 127.0.0.1. HTTP header that caused checking: x-vp-sender-id"));
		}
	}

	@Test
	public void testVP011IsThrownWhenCallerIsNotOnWhitelistUsingHeader_X_VP_CERT()
			throws Exception {
		/*
		 * Provide a valid cert in http header x-vp-auth-cert to trigger a check
		 * on the ip whitelist.
		 */
		Map<String, String> properties = new HashMap<String, String>();
		properties
				.put(HttpHeaders.REVERSE_PROXY_HEADER_NAME, clientCertificate);

		try {
			testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_HTTP,
					LOGICAL_ADDRESS, properties);
			fail("Expected error here!");
		} catch (Exception ex) {
			assertTrue(ex
					.getMessage()
					.contains(
							"VP011 Caller was not on the white list of accepted IP-addresses. IP-address: 127.0.0.1. HTTP header that caused checking: x-vp-auth-cert"));
		}
	}

	@Test
	public void testVP011IsThrownWhenCallerIsNotOnWhitelistUsingReverseProxyTerminatingTls()
			throws Exception {
		// add headers set by fronting reverse proxy that terminates TLS
		Map<String, String> properties = new HashMap<String, String>();
		properties
				.put(HttpHeaders.REVERSE_PROXY_HEADER_NAME, clientCertificate);
		properties.put("X-Forwarded-For", "10.10.10.10");

		try {
			testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_HTTP,
					LOGICAL_ADDRESS, properties);
			fail("expected exception!");
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(e
					.getMessage()
					.contains(
							"VP011 Caller was not on the white list of accepted IP-addresses. IP-address: 127.0.0.1. HTTP header that caused checking: x-vp-auth-cert"));
		}
	}

}