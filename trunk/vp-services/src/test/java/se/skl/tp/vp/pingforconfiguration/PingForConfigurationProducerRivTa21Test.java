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
package se.skl.tp.vp.pingforconfiguration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import se.skl.tp.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skl.tp.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;
import se.skl.tp.vp.vagvalagent.VagvalAgent;

public class PingForConfigurationProducerRivTa21Test {
	
	public static List<AnropsBehorighetsInfoType> BEHORIGHETS_INFO_TYPES = new ArrayList<AnropsBehorighetsInfoType>();
	public static List<VirtualiseringsInfoType> VIRTUALISERINGS_INFO_TYPES = new ArrayList<VirtualiseringsInfoType>();
	
	@BeforeClass
	public static void beforeClass(){
		BEHORIGHETS_INFO_TYPES.add(createAnropsBehorighetsInfo());
		VIRTUALISERINGS_INFO_TYPES.add(createVirtualiseringsInfo());
	}
	
	@Test
	public void resourcesNeededForVpAvailable() {	
		
		VagvalAgent vagvalAgent = mock(VagvalAgent.class);
		when(vagvalAgent.getAnropsBehorighetsInfoList()).thenReturn(BEHORIGHETS_INFO_TYPES);
		when(vagvalAgent.getVirtualiseringsInfo()).thenReturn(VIRTUALISERINGS_INFO_TYPES);
		
		PingForConfigurationProducerRivTa21 pingRivTa21 = new PingForConfigurationProducerRivTa21();
		pingRivTa21.setVagvalAgent(vagvalAgent);
		
		assertTrue(pingRivTa21.resourcesNeededForVpAvailable());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void virtualiseringsInfoIsMissing() {
		
		VagvalAgent vagvalAgent = mock(VagvalAgent.class);
		when(vagvalAgent.getAnropsBehorighetsInfoList()).thenReturn(BEHORIGHETS_INFO_TYPES);
		when(vagvalAgent.getVirtualiseringsInfo()).thenReturn(Collections.EMPTY_LIST);
		
		PingForConfigurationProducerRivTa21 pingRivTa21 = new PingForConfigurationProducerRivTa21();
		pingRivTa21.setVagvalAgent(vagvalAgent);
		
		assertFalse(pingRivTa21.resourcesNeededForVpAvailable());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void behorighetsInfoIsMissing() {
		
		VagvalAgent vagvalAgent = mock(VagvalAgent.class);
		when(vagvalAgent.getAnropsBehorighetsInfoList()).thenReturn(Collections.EMPTY_LIST);
		when(vagvalAgent.getVirtualiseringsInfo()).thenReturn(VIRTUALISERINGS_INFO_TYPES);
		
		PingForConfigurationProducerRivTa21 pingRivTa21 = new PingForConfigurationProducerRivTa21();
		pingRivTa21.setVagvalAgent(vagvalAgent);
		
		assertFalse(pingRivTa21.resourcesNeededForVpAvailable());
	}
	
	@Test
	public void agentIsMissing() {
		
		PingForConfigurationProducerRivTa21 pingRivTa21 = new PingForConfigurationProducerRivTa21();
		pingRivTa21.setVagvalAgent(null);
		
		assertFalse(pingRivTa21.resourcesNeededForVpAvailable());
	}
	
	private static VirtualiseringsInfoType createVirtualiseringsInfo() {
		VirtualiseringsInfoType infoType = new VirtualiseringsInfoType();
		return infoType;
	}

	private static AnropsBehorighetsInfoType createAnropsBehorighetsInfo() {
		AnropsBehorighetsInfoType infoType = new AnropsBehorighetsInfoType();
		return infoType;
	}

}
