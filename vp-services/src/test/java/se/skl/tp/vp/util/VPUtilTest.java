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
package se.skl.tp.vp.util;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.mockito.Mockito.*;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;

import se.skl.tp.vp.vagvalrouter.ExceptionMessageTransformer;

public class VPUtilTest {
	
	public static final String WHITE_LIST="127.0.0.1,127.0.0.2,127.0.0.3";
	public static final String WHITE_LIST_WITH_COMMENT="127.0.0.1,127.0.0.2,127.0.0.3 #Inline comment";
	public static final String REMOTE_ADDRESS = "/127.0.0.1:52440";
	
	final static String CORRECT_FORMATED_SOAP_FAULT = 
			"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
			"  <soapenv:Header/>" + 
			"  <soapenv:Body>" + 
			"    <soap:Fault xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
			"      <faultcode>soap:Server</faultcode>\n" + 
			"      <faultstring>%s</faultstring>\n" +
			"    </soap:Fault>" + 
			"  </soapenv:Body>" + 
			"</soapenv:Envelope>";

	@Test
	public void extractIpAdressFromRemoteClientAddress() {
		
		MuleMessage message = mock(MuleMessage.class);
		when(message.getProperty(VPUtil.REMOTE_ADDR, PropertyScope.INBOUND)).thenReturn(REMOTE_ADDRESS);
		
		String ipAddress = VPUtil.extractIpAddress(message);
		assertEquals("127.0.0.1", ipAddress);
	}
	
	@Test
	public void isCallerOnWhiteListOk(){
				
		boolean callerOnWhiteList = VPUtil.isCallerOnWhiteList("127.0.0.1", WHITE_LIST, HttpHeaders.X_VP_SENDER_ID);
		assertTrue(callerOnWhiteList);
	}

	@Test
	public void isCallerOnWhiteListOkWithComment(){
				
		boolean callerOnWhiteList = VPUtil.isCallerOnWhiteList("127.0.0.1", WHITE_LIST_WITH_COMMENT, HttpHeaders.X_VP_SENDER_ID);
		assertTrue(callerOnWhiteList);
	}
	

	@Test
	public void isCallerOnWhiteListOkWhenWhiteListContainsLeadingWiteSpaces(){
		
		final String WHITE_LIST_WITH_WHITE_SPACE="127.0.0.1, 127.0.0.2";
		boolean callerOnWhiteList = VPUtil.isCallerOnWhiteList("127.0.0.2 ", WHITE_LIST_WITH_WHITE_SPACE, HttpHeaders.X_VP_SENDER_ID);
		assertTrue(callerOnWhiteList);
	}		
	
	@Test
	public void isCallerOnWhiteListIpDoesNotMatch(){
				
		boolean callerOnWhiteList = VPUtil.isCallerOnWhiteList("126.0.0.1", WHITE_LIST, HttpHeaders.X_VP_SENDER_ID);
		assertFalse(callerOnWhiteList);
	}
	
	@Test
	public void isCallerOnWhiteListMatchesSubdomain(){
		
		String whiteListOfSubDomains = "127.0.0,127.0.1.0";		
		boolean callerOnWhiteList = VPUtil.isCallerOnWhiteList("127.0.0.1", whiteListOfSubDomains, HttpHeaders.X_VP_SENDER_ID);
		assertTrue(callerOnWhiteList);
	}
	
	@Test
	public void isCallerOnWhiteListDoesNotMatchSubdomain(){
		
		String whiteListOfSubDomains = "127.0.0,127.0.1";		
		boolean callerOnWhiteList = VPUtil.isCallerOnWhiteList("127.0.2.1", whiteListOfSubDomains, HttpHeaders.X_VP_SENDER_ID);
		assertFalse(callerOnWhiteList);
	}
	
	@Test
	public void isCallerOnWhiteListReturnsFalseWhenIpAddressIsEmpty(){		
		
		String ipAddress = "";
		assertFalse(VPUtil.isCallerOnWhiteList(ipAddress, WHITE_LIST, HttpHeaders.X_VP_SENDER_ID));
	}
	
	@Test
	public void isCallerOnWhiteListReturnsFalseWhenIpAddressIsNull(){		
		
		String ipAddress = null;
		assertFalse(VPUtil.isCallerOnWhiteList(ipAddress, WHITE_LIST, HttpHeaders.X_VP_SENDER_ID));
	}
	
	@Test
	public void isCallerOnWhiteListReturnsFalseWhenWhiteListIsEmpty(){
			
		String whiteList = "";
		assertFalse(VPUtil.isCallerOnWhiteList("127.0.0.1", whiteList, HttpHeaders.X_VP_SENDER_ID));
	}
	
	@Test
	public void isCallerOnWhiteListReturnsFalseWhenWhiteListIsNull(){
			
		String whiteList = null;
		assertFalse(VPUtil.isCallerOnWhiteList("127.0.0.1", whiteList, HttpHeaders.X_VP_SENDER_ID));
	}
	
	


	@Test
	public void transformToSoapFault_ok() {
		String cause = "VP004 No Logical Adress found for serviceNamespace:urn:riv:domain:subdomain:GetProductDetailResponder:1, receiverId:vp-test-producer_kalle";
		String expectedResult = String.format(CORRECT_FORMATED_SOAP_FAULT, cause);
		
		String actualResult = VPUtil.generateSoap11FaultWithCause(cause);
		
		assertNotNull(actualResult);
		assertEquals(expectedResult, actualResult);
	}

}
