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
package se.skl.tp.vp.mule.bugfix;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.module.xml.transformer.XmlToXMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * #MULE-370: bug workaround for Mule-3.7.0 issues: MULE-8913, MULE-8508.
 */
public class XmlToXMLStreamReader_BugfixedForMule370 extends
		XmlToXMLStreamReader implements DiscoverableTransformer {

	private static final Logger log = LoggerFactory
			.getLogger(XmlToXMLStreamReader_BugfixedForMule370.class);

	// make sure this transformer is preferred over the non-bug-fixed one
	private int priorityWeighting = DiscoverableTransformer.MAX_PRIORITY_WEIGHTING;

	@Override
	public int getPriorityWeighting() {
		return priorityWeighting;
	}

	@Override
	public void setPriorityWeighting(int priorityWeighting) {
		this.priorityWeighting = priorityWeighting;
	}

	public XmlToXMLStreamReader_BugfixedForMule370() {
		super();
		log.debug("invoked constructor");
		try {
			initialise();
		} catch (InitialisationException e) {
			throw new RuntimeException("Could not initialise super class", e);
		}
	}

}
