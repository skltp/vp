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
package se.skl.tp.vp.tjanstekatalogen.producer;

import static se.skl.tp.vp.util.VagvalSchemasTestUtil.AN_HOUR_AGO;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.IN_TEN_YEARS;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.getRelativeDate;

import javax.xml.datatype.XMLGregorianCalendar;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoIdType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.HamtaAllaAnropsBehorigheterResponseType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.HamtaAllaVirtualiseringarResponseType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.SokVagvalsInfoInterface;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoIdType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.HamtaAllaTjanstekontraktResponseType;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;
import se.skl.tp.vp.vagvalrouter.VagvalInput;

/**
 * Denna klass används för att kunna simulera en tjänstekatalog med valfritt
 * innehåll. Används ihop med tk-teststub-service.xml så att det
 * skapas en enpoint som svarar när vägvalsagenten anropar tjänstekatalogen
 * för att få en lista på alla vägval tp-vitualisering-DEV använder sig av
 * denna.
 *
 * Se exempel i VPFullServiceTest.java eller VPMuleServer.java hur den kan användas.
 */

@javax.jws.WebService(portName = "SokVagvalsSoap11LitDocPort", serviceName = "SokVagvalsServiceSoap11LitDocService", targetNamespace = "urn:skl:tp:vagvalsinfo:v2")
public class TjansteKatalogenTestProducer implements SokVagvalsInfoInterface {

	private SokVagvalsInfoMockInput vagvalInputs;

	public void setVagvalInputs(SokVagvalsInfoMockInput vagvalInputs) {
		this.vagvalInputs = vagvalInputs;
	}

	public HamtaAllaTjanstekontraktResponseType hamtaAllaTjanstekontrakt(Object parameters){
		return null;
	}

	/**
	 * Hämta en lista av alla anropsbehörigheter.
	 *
	 * @param parameters
	 *            - null, eftersom operationen inte har någon payload.
	 */
	public HamtaAllaAnropsBehorigheterResponseType hamtaAllaAnropsBehorigheter(Object parameters) {

		HamtaAllaAnropsBehorigheterResponseType sampleResponse = new HamtaAllaAnropsBehorigheterResponseType();

		try {
			int id = 1;

			for (VagvalMockInputRecord input : vagvalInputs.getVagvalInputs()) {
				AnropsBehorighetsInfoIdType aboId = new AnropsBehorighetsInfoIdType();
				aboId.setValue(String.valueOf(id++));
				AnropsBehorighetsInfoType abo = new AnropsBehorighetsInfoType();
				abo.setAnropsBehorighetsInfoId(aboId);
				XMLGregorianCalendar fromDate = input.getFromDate();
				XMLGregorianCalendar toDate = input.getToDate();
				abo.setFromTidpunkt(fromDate==null ? getRelativeDate(AN_HOUR_AGO) : fromDate);
				abo.setTomTidpunkt(toDate==null ? getRelativeDate(IN_TEN_YEARS) : toDate);
				abo.setReceiverId(input.receiverId);
				abo.setSenderId(input.senderId);
				abo.setTjansteKontrakt(input.serviceContractNamespace);
				sampleResponse.getAnropsBehorighetsInfo().add(abo);
			}

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
		HamtaAllaVirtualiseringarResponseType sampleResponse = new HamtaAllaVirtualiseringarResponseType();

		try {
			int id = 1;

			for (VagvalMockInputRecord input : vagvalInputs.getVagvalInputs()) {
				VirtualiseringsInfoType vi = new VirtualiseringsInfoType();
				vi.setAdress(input.adress);
				XMLGregorianCalendar fromDate = input.getFromDate();
				XMLGregorianCalendar toDate = input.getToDate();
				vi.setFromTidpunkt(fromDate==null ? getRelativeDate(AN_HOUR_AGO) : fromDate);
				vi.setTomTidpunkt(toDate==null ? getRelativeDate(IN_TEN_YEARS) : toDate);
				vi.setReceiverId(input.receiverId);
				vi.setRivProfil(input.rivVersion);
				VirtualiseringsInfoIdType viId = new VirtualiseringsInfoIdType();
				viId.setValue(String.valueOf(id++));
				vi.setVirtualiseringsInfoId(viId);
				vi.setTjansteKontrakt(input.serviceContractNamespace);
				sampleResponse.getVirtualiseringsInfo().add(vi);

			}

		} catch (Exception e) {
			throw new RuntimeException("Technical failure: " + e.getMessage(), e);
		}

		return sampleResponse;
	}

}
