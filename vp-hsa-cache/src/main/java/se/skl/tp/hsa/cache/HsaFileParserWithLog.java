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
package se.skl.tp.hsa.cache;

import java.io.PrintWriter;

/**
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
public class HsaFileParserWithLog extends HsaFileParser {

	/**
	 * PrintWriter to use for printing errors
	 */
	private PrintWriter pw;

	/**
	 * Constructor
	 * 
	 * @param pw {@link PrintWriter}
	 */
	public HsaFileParserWithLog(PrintWriter pw){
		this.pw = pw;
	}
	
	/*
	 * (non-Javadoc)
	 * @see se.skl.tp.hsa.cache.HsaFileParser#logError(java.lang.String)
	 */
	@Override
	protected void logError(String msg) {
		this.pw.println("ERROR " + msg);
	}
}
