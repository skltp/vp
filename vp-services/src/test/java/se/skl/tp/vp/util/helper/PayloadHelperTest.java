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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import se.skl.tp.vp.util.helper.PayloadHelper.PayloadInfo;

public class PayloadHelperTest extends AbstractMuleContextTestCase {

	/**
	 * Test that we can get a logicalAdress from a request in the format
	 *  <urn1:To>RIVTA20REQUEST</urn1:To>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExtractLogicalAdressInToFormat() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/Rivta20Request.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());
		ReversibleXMLStreamReader xmlStreamReader = new ReversibleXMLStreamReader(xstream);
		
		MuleMessage testMsg = new DefaultMuleMessage(xmlStreamReader, muleContext);

		PayloadHelper helper = new PayloadHelper(testMsg);
		
		PayloadInfo result = helper.extractInfoFromPayload();
		assertTrue(result.receiverId.equalsIgnoreCase("RIVTA20REQUEST"));		
	}
	
	/**
	 * Test that we can get a logicalAdress from a request in the format
	 *    <urn1:LogicalAddress>RIVTA20REQUEST</urn1:LogicalAddress>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExtractLogicalAdressInLogicalAddressFormat() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/Rivta21Request.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());
		ReversibleXMLStreamReader xmlStreamReader = new ReversibleXMLStreamReader(xstream);
		
		MuleMessage testMsg = new DefaultMuleMessage(xmlStreamReader, muleContext);

		PayloadHelper helper = new PayloadHelper(testMsg);
		PayloadInfo result = helper.extractInfoFromPayload();
		assertTrue(result.receiverId.equalsIgnoreCase("RIVTA21REQUEST"));
	}

	/**
	 * Test that we cannot get a logicalAdress from a request in the format
	 *  <urn1:To></urn1:To>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExtractEmptyLogicalAdress() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/Rivta20RequestNoToData.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());
		ReversibleXMLStreamReader xmlStreamReader = new ReversibleXMLStreamReader(xstream);
		
		MuleMessage testMsg = new DefaultMuleMessage(xmlStreamReader, muleContext);

		PayloadHelper helper = new PayloadHelper(testMsg);
		PayloadInfo result = helper.extractInfoFromPayload();
    assertNull(result.receiverId);
	}

	/**
	 * Test that we cannot get a logicalAdress from a request in the format
	 *  <urn1:To>   </urn1:To>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExtractSpaceLogicalAdress() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/Rivta20RequestSpaceToData.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());
		ReversibleXMLStreamReader xmlStreamReader = new ReversibleXMLStreamReader(xstream);
		
		MuleMessage testMsg = new DefaultMuleMessage(xmlStreamReader, muleContext);

		PayloadHelper helper = new PayloadHelper(testMsg);
		PayloadInfo result = helper.extractInfoFromPayload();
		assertTrue(result.receiverId == null);	
	}

	/**
	 * Test that we can get a logicalAdress from a request in the format
	 *  <urn1:To> RIVTA20REQUEST</urn1:To>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExtractSpaceStartLogicalAdress() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/Rivta20RequestStartSpaceToData.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());
		ReversibleXMLStreamReader xmlStreamReader = new ReversibleXMLStreamReader(xstream);
		
		MuleMessage testMsg = new DefaultMuleMessage(xmlStreamReader, muleContext);

		PayloadHelper helper = new PayloadHelper(testMsg);
		PayloadInfo result = helper.extractInfoFromPayload();
		assertTrue(result.receiverId.equalsIgnoreCase(" RIVTA20REQUEST"));
	}

	/**
	 * Test that we can get a logicalAdress from a request in the format
	 *  <urn1:To>RIVTA20REQUEST </urn1:To>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExtractSpaceEndLogicalAdress() throws Exception {
		final URL resource = Thread.currentThread().getContextClassLoader()
				.getResource("testfiles/Rivta20RequestEndSpaceToData.xml");
		final XMLStreamReader xstream = XMLInputFactory.newInstance().createXMLStreamReader(resource.openStream());
		ReversibleXMLStreamReader xmlStreamReader = new ReversibleXMLStreamReader(xstream);
		
		MuleMessage testMsg = new DefaultMuleMessage(xmlStreamReader, muleContext);

		PayloadHelper helper = new PayloadHelper(testMsg);
		PayloadInfo result = helper.extractInfoFromPayload();
		assertTrue(result.receiverId.equalsIgnoreCase("RIVTA20REQUEST "));
	}

	
}
