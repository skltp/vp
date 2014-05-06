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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VagvalInfo {

	private static Logger log = LoggerFactory.getLogger(VagvalInfo.class);

	public static class Info {
		
		public String sender;
		public String receiver;
		public String tjansteKontrakt;
		public String rivVersion;
		public String adress;

		Info(String receiver, String sender, String rivVersion, String tjansteKontrakt, String adress) {
			this.receiver = receiver;
			this.sender = sender;
			this.rivVersion = rivVersion;
			this.tjansteKontrakt = tjansteKontrakt;
			this.adress = adress;
		}
	}

	private static List<Info> infos = new ArrayList<Info>();

	public List<Info> getInfos() {
		log.debug("Get vagval-info, return {} records", infos.size());
		return infos;
	}
	
	public void addVagval(String receiver, String sender, String rivVersion, String tjansteKontrakt, String adress){
		log.debug("Add one vagval-info record");
		infos.add(new Info(receiver,sender,rivVersion,tjansteKontrakt,adress));
	}

	public void reset() {
		log.debug("Reset vagval-info");
		infos = new ArrayList<Info>();		
	}
	
}

