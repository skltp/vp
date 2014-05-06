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
package se.skl.tp.vp.vagvalagent;

import java.util.List;

import se.skl.tp.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skl.tp.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;

public class VagvalAgentMock extends VagvalAgent {

	private List<VirtualiseringsInfoType> virtualiseringsInfo;
	private List<AnropsBehorighetsInfoType> anropsBehorighetsInfo;

	public VagvalAgentMock (List<VirtualiseringsInfoType> virtualiseringsInfo, List<AnropsBehorighetsInfoType> anropsBehorighetsInfo) {
		this.virtualiseringsInfo = virtualiseringsInfo;
		this.anropsBehorighetsInfo = anropsBehorighetsInfo;
	}
	
	public VagvalAgentMock (List<AnropsBehorighetsInfoType> anropsBehorighetsInfo) {
		this(null, anropsBehorighetsInfo);
	}
	
	public VagvalAgentMock () {
		this(null, null);
	}
	
	@Override
	protected List<VirtualiseringsInfoType> getVirtualiseringar() {
		return virtualiseringsInfo;
	}
	
	@Override
	protected List<AnropsBehorighetsInfoType> getBehorigheter() {
		return anropsBehorighetsInfo;
	}
	
	@Override
	public List<AnropsBehorighetsInfoType> getAnropsBehorighetsInfoList() {
		return anropsBehorighetsInfo;
	}

	/*
	 * MOCK SPECIFIC METHODS
	 */
	public List<VirtualiseringsInfoType> getMockVirtualiseringsInfo() {
		return virtualiseringsInfo;
	}
	public void setMockVirtualiseringsInfo(List<VirtualiseringsInfoType> virtualiseringsInfo) {
		this.virtualiseringsInfo = virtualiseringsInfo;
	}
	
	public List<AnropsBehorighetsInfoType> getMockAnropsBehorighetsInfo() {
		return anropsBehorighetsInfo;
	}
	public void setMockAnropsBehorighetsInfo(List<AnropsBehorighetsInfoType> anropsBehorighetsInfo) {
		this.anropsBehorighetsInfo = anropsBehorighetsInfo;
	}

}
