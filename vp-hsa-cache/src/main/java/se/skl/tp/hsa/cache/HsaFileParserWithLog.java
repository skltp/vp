/**
 * Copyright 2013 Sjukvardsradgivningen
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
