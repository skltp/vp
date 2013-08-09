/**
 * Copyright (c) 2013 Sveriges Kommuner och Landsting (SKL).
 * 								<http://www.skl.se/>
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
package se.skl.tp.vp.deployer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Simple tests.
 * 
 * @author Peter
 */
public class DeployerMainTest {

	private DeployerMain dm;
	
	@Before
	public void before() {
		dm = new DeployerMain();
	}
	
	@Test
	public void updateDeploy() throws Exception {
		dm.deploy("src/test/resources/test.jar", true);
	}	
	
	@Test
	public void replaceColonsInNamespaceWithDots() throws Exception {
		DeployerMain.XsdInfo xsdInfo = new DeployerMain.XsdInfo("urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1");
		Assert.assertEquals("urn.riv.crm.scheduling.GetSubjectOfCareScheduleResponder.1",xsdInfo.getNamespaceSeparatedWith("."));
	}	
	
	@Test
	public void extractResponderFromService(){
		Assert.assertEquals("GetSubjectOfCareScheduleResponder", DeployerMain.extractResponderNameFromServiceName("GetSubjectOfCareScheduleResponderService"));
		Assert.assertEquals("GetLogicalAddresseesByServiceContractResponder", DeployerMain.extractResponderNameFromServiceName("GetLogicalAddresseesByServiceContractResponderService"));
		Assert.assertEquals("ServiceSomethingResponder", DeployerMain.extractResponderNameFromServiceName("ServiceSomethingResponderService"));
	}
}
