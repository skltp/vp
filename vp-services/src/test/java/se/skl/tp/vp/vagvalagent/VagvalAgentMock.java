/**
 * Copyright 2009 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.vp.vagvalagent;

import java.util.List;

import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;

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
