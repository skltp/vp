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
import se.skl.tp.behorighet.BehorighetHandlerImpl;
import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.vagval.VagvalHandlerImpl;
import se.skl.tp.vp.util.MessageProperties;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;
import se.skltp.takcache.TakCacheImpl;

public class VagvalAgentOldMock extends VagvalAgentOld {

	private List<VirtualiseringsInfoType> virtualiseringsInfo;
	private List<AnropsBehorighetsInfoType> anropsBehorighetsInfo;

	private TakServiceMock takServiceMock;
	private se.skltp.takcache.TakCacheImpl takCache;

	public VagvalAgentOldMock(List<VirtualiseringsInfoType> virtualiseringsInfo, List<AnropsBehorighetsInfoType> anropsBehorighetsInfo) {
		this.virtualiseringsInfo = virtualiseringsInfo;
		this.anropsBehorighetsInfo = anropsBehorighetsInfo;
	    this.setMessageProperties(MessageProperties.getInstance());

	    System.setProperty("takcache.use.behorighet.cache", "true");
	    System.setProperty("takcache.use.vagval.cache", "true");

		takServiceMock = new TakServiceMock();
		takServiceMock.setAnropsBehorighetsInfo(anropsBehorighetsInfo);
		takServiceMock.setVirtualiseringsInfo(virtualiseringsInfo);
		takCache = new TakCacheImpl(takServiceMock);
		takCache.setUseVagvalCache(true);
		takCache.setUseBehorighetCache(true);

	}

	public VagvalAgentOldMock(List<AnropsBehorighetsInfoType> anropsBehorighetsInfo) {
		this(null, anropsBehorighetsInfo);
	}

	public VagvalAgentOldMock() {
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
		takServiceMock.setVirtualiseringsInfo(virtualiseringsInfo);
		this.virtualiseringsInfo = virtualiseringsInfo;
	}

	public List<AnropsBehorighetsInfoType> getMockAnropsBehorighetsInfo() {
		return anropsBehorighetsInfo;
	}
	public void setMockAnropsBehorighetsInfo(List<AnropsBehorighetsInfoType> anropsBehorighetsInfo) {
		takServiceMock.setAnropsBehorighetsInfo(anropsBehorighetsInfo);
		this.anropsBehorighetsInfo = anropsBehorighetsInfo;
	}

	public void createBehorighetHandler( HsaCache hsaCache, String delimiter){
		setBehorighetHandler(new BehorighetHandlerImpl(hsaCache, takCache,delimiter));

	}

	public void createVagvalHandler(HsaCache hsaCache, String delimiter){
		setVagvalHandler(new VagvalHandlerImpl(hsaCache, takCache, delimiter));
	}

}
