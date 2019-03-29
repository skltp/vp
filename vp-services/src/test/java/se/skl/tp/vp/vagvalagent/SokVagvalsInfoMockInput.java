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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SokVagvalsInfoMockInput {
	
	private static List<VagvalMockInputRecord> _vagvalInputs;
	private static Logger log = LoggerFactory.getLogger(SokVagvalsInfoMockInput.class);

	public void reset() {
		log.debug("Reset mock-vagval-info");
		_vagvalInputs = null;
	}

	public List<VagvalMockInputRecord> getVagvalInputs() {
		log.debug("Get mock-vagval-info, return {} records", ((_vagvalInputs == null) ? "NULL" : _vagvalInputs.size()));
		if(_vagvalInputs == null){
			_vagvalInputs = new ArrayList<>();
		}
		return _vagvalInputs;
	}

	public void setVagvalInputs(List<VagvalMockInputRecord> vagvalInputs) {
		log.debug("Set mock-vagval-info, with {} records", ((vagvalInputs == null) ? "NULL" : vagvalInputs.size()));
		_vagvalInputs = vagvalInputs;
	}
}