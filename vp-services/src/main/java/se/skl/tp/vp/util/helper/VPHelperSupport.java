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
package se.skl.tp.vp.util.helper;

import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class VPHelperSupport {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	private MuleMessage muleMessage;
	private Pattern pattern;
	private String whiteList;
	
	public VPHelperSupport(final MuleMessage muleMessage, final Pattern pattern, final String whiteList) {
		this.muleMessage = muleMessage;
		this.pattern = pattern;
		this.whiteList = whiteList;
	}
	
	public MuleMessage getMuleMessage() {
		return this.muleMessage;
	}
	
	public Pattern getPattern() {
		return this.pattern;
	}
	
	public String getWhiteList() {
		return this.whiteList;
	}
	
	protected Logger getLog() {
		return this.log;
	}
}
