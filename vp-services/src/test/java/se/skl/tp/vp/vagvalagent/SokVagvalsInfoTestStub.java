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

import static se.skl.tp.vp.util.VagvalSchemasTestUtil.AN_HOUR_AGO;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.IN_TEN_YEARS;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.getRelativeDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoIdType;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;
import se.skl.tp.vagvalsinfo.wsdl.v1.HamtaAllaAnropsBehorigheterResponseType;
import se.skl.tp.vagvalsinfo.wsdl.v1.HamtaAllaVirtualiseringarResponseType;
import se.skl.tp.vagvalsinfo.wsdl.v1.SokVagvalsInfoInterface;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoIdType;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;
import se.skl.tp.vp.vagvalrouter.VagvalInfo;

/**
 * Denna klass används för att kunna simulera en tjänstekatalog med valfritt
 * innehåll. Används ihop med tp-vagval-agent-teststub-config så att det skapas
 * en enpoint som svarar när vägvalsagenten anropar tjänstekatalogen för att få
 * en lista på alla vägval tp-vitualisering-DEV använder sig av denna.
 */

@javax.jws.WebService(portName = "SokVagvalsSoap11LitDocPort", serviceName = "SokVagvalsServiceSoap11LitDocService", targetNamespace = "urn:skl:tp:vagvalsinfo:v1")
public class SokVagvalsInfoTestStub implements SokVagvalsInfoInterface {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private VagvalInfo _vagvalInfo;

	public void setVagvalInfo(VagvalInfo vagvalInfo) {
		this._vagvalInfo = vagvalInfo;
	}

	private VagvalInfo getVagvalInfo() {
		if (_vagvalInfo == null) {
			_vagvalInfo = initVagvalInfo();
		}
		return _vagvalInfo;
	}
	private VagvalInfo initVagvalInfo() {
		logger.info("TK-teststub initiates valvalInfo");
		VagvalInfo vi = new VagvalInfo();
		vi.addVagval("vp-test-producer", "tp", "RIVTABP20", "urn:skl:tjanst1:rivtabp20", "https://localhost:19000/vardgivare-b/tjanst1");
		logger.info("TK-teststub vagvalInfo now contains {} records", vi.getInfos().size());		
		return vi;
	}

	/**
	 * Hämta en lista av alla anropsbehörigheter.
	 * 
	 * @param parameters
	 *            - null, eftersom operationen inte har någon payload.
	 */
	public HamtaAllaAnropsBehorigheterResponseType hamtaAllaAnropsBehorigheter(Object parameters) {

		logger.info("TK-teststub start hamtaAllaAnropsBehorigheter()");
		HamtaAllaAnropsBehorigheterResponseType sampleResponse = new HamtaAllaAnropsBehorigheterResponseType();

		try {
			int id = 1;
			for (VagvalInfo.Info vagval : getVagvalInfo().getInfos()) {
				AnropsBehorighetsInfoIdType aboId = new AnropsBehorighetsInfoIdType();
				aboId.setValue(String.valueOf(id++));
				AnropsBehorighetsInfoType abo = new AnropsBehorighetsInfoType();
				abo.setAnropsBehorighetsInfoId(aboId);
				abo.setFromTidpunkt(getRelativeDate(AN_HOUR_AGO));
				abo.setTomTidpunkt(getRelativeDate(IN_TEN_YEARS));
				abo.setReceiverId(vagval.receiver);
				abo.setSenderId(vagval.sender);
				abo.setTjansteKontrakt(vagval.tjansteKontrakt);
				sampleResponse.getAnropsBehorighetsInfo().add(abo);
			}

			logger.info("TK-teststub hamtaAllaAnropsBehorigheter() returns {} records", sampleResponse.getAnropsBehorighetsInfo().size());

		} catch (Exception e) {
			throw new RuntimeException("Technical failure: " + e.getMessage(), e);
		}

		return sampleResponse;
	}

	/**
	 * Hämta en lista av alla virtualiseringar.
	 * 
	 * @param parameters
	 *            - null, eftersom operationen inte har någon payload.
	 */
	public HamtaAllaVirtualiseringarResponseType hamtaAllaVirtualiseringar(Object parameters) {

		logger.info("TK-teststub start hamtaAllaVirtualiseringar()");

		HamtaAllaVirtualiseringarResponseType sampleResponse = new HamtaAllaVirtualiseringarResponseType();

		try {
			int id = 1;
			for (VagvalInfo.Info vagval : getVagvalInfo().getInfos()) {
				VirtualiseringsInfoType vi = new VirtualiseringsInfoType();
				vi.setAdress(vagval.adress);
				vi.setFromTidpunkt(getRelativeDate(AN_HOUR_AGO));
				vi.setTomTidpunkt(getRelativeDate(IN_TEN_YEARS));
				vi.setReceiverId(vagval.receiver);
				vi.setRivProfil(vagval.rivVersion);
				VirtualiseringsInfoIdType viId = new VirtualiseringsInfoIdType();
				viId.setValue(String.valueOf(id++));
				vi.setVirtualiseringsInfoId(viId);
				vi.setTjansteKontrakt(vagval.tjansteKontrakt);
				sampleResponse.getVirtualiseringsInfo().add(vi);
			}

		} catch (Exception e) {
			throw new RuntimeException("Technical failure: " + e.getMessage(), e);
		}

		logger.info("TK-teststub hamtaAllaVirtualiseringar() returns {} records", sampleResponse.getVirtualiseringsInfo().size());

		return sampleResponse;
	}

}

