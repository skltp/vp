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
package se.skl.tp.vp.vagvalrouter;

import java.util.ArrayList;
import java.util.List;

public class VagvalInfo {

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

	private List<Info> infos = new ArrayList<Info>();

	public List<Info> getInfos() {
		return infos;
	}
	
	public void addVagval(String receiver, String sender, String rivVersion, String tjansteKontrakt, String adress){
		infos.add(new Info(receiver,sender,rivVersion,tjansteKontrakt,adress));
	}

	public void reset() {
		infos = new ArrayList<Info>();		
	}
	
}

