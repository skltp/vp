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

public class VPUtilTest {
	
	public static final String WHITE_LIST="127.0.0.1,127.0.0.2,127.0.0.3";
	public static final String REMOTE_ADDRESS = "/127.0.0.1:52440";

	@Test
	public void extractIpAdressFromRemoteClientAddress() {
		
		MuleMessage message = mock(MuleMessage.class);
		when(message.getProperty(VPUtil.REMOTE_ADDR, PropertyScope.INBOUND)).thenReturn(REMOTE_ADDRESS);
		
		String ipAddress = VPUtil.extractIpAddress(message);
		assertEquals("127.0.0.1", ipAddress);
	}
	
	@Test
	public void isCallerOnWhiteListOk(){
				
		boolean callerOnWhiteList = VPUtil.isCallerOnWhiteList("127.0.0.1", WHITE_LIST, VPUtil.SENDER_ID);
		assertTrue(callerOnWhiteList);
	}
	
	@Test
	public void isCallerOnWhiteListIpDoesNotMatch(){
				
		boolean callerOnWhiteList = VPUtil.isCallerOnWhiteList("126.0.0.1", WHITE_LIST, VPUtil.SENDER_ID);
		assertFalse(callerOnWhiteList);
	}
	
	@Test
	public void isCallerOnWhiteListMatchesSubdomain(){
		
		String whiteListOfSubDomains = "127.0.0,127.0.1.0";		
		boolean callerOnWhiteList = VPUtil.isCallerOnWhiteList("127.0.0.1", whiteListOfSubDomains, VPUtil.SENDER_ID);
		assertTrue(callerOnWhiteList);
	}
	
	@Test
	public void isCallerOnWhiteListDoesNotMatchSubdomain(){
		
		String whiteListOfSubDomains = "127.0.0,127.0.1";		
		boolean callerOnWhiteList = VPUtil.isCallerOnWhiteList("127.0.2.1", whiteListOfSubDomains, VPUtil.SENDER_ID);
		assertFalse(callerOnWhiteList);
	}
	
	@Test
	public void isCallerOnWhiteListThrowsExceptionWhenIpAdressIsEmpty(){
			
		try {
			VPUtil.isCallerOnWhiteList("", WHITE_LIST, VPUtil.SENDER_ID);
			fail("Expected fail");
		} catch (Exception e) {
			assertEquals("Could not extract the IP address of the caller. Cannot check whether caller is on the white list. HTTP header that caused checking: senderid", e.getMessage());
		}
	}
	
	@Test
	public void isCallerOnWhiteListThrowsExceptionWhenWhiteListIsEmpty(){
			
		try {
			VPUtil.isCallerOnWhiteList("127.0.0.1", "", VPUtil.SENDER_ID);
			fail("Expected fail");
		} catch (Exception e) {
			assertEquals("Could not check whether the caller is on the white list because the white list was empty. HTTP header that caused checking: senderid", e.getMessage());
		}
	}
	
	@Test
	public void isCallerOnWhiteListThrowsExceptionWhenWhiteListIsNull(){
			
		try {
			VPUtil.isCallerOnWhiteList("127.0.0.1", null, VPUtil.SENDER_ID);
			fail("Expected fail");
		} catch (Exception e) {
			assertEquals("Could not check whether the caller is on the white list because the white list was empty. HTTP header that caused checking: senderid", e.getMessage());
		}
	}

}
