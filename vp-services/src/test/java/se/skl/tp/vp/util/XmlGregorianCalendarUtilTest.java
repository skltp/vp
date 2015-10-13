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
package se.skl.tp.vp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Ignore;
import org.junit.Test;

public class XmlGregorianCalendarUtilTest {
	
	@Ignore("only for performance testing for issue SKLTP-741")
	@Test
	public void performanceTest_getNowAsXMLGregorianCalendar_issue_SKLTP_741() {

		long t1 = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		}
		long t2 = System.currentTimeMillis();
		System.out.println(t2 - t1);
	}

	@Test
	public void testFromDate() {
		Date testDate = new Date();

		XMLGregorianCalendar xmlDate = XmlGregorianCalendarUtil
				.fromDate(testDate);

		assertEquals(testDate.getTime(), xmlDate.toGregorianCalendar()
				.getTime().getTime());
	}

	@Test
	public void testGetNowAsXMLGregorianCalendar() {
		long tsBefore = System.currentTimeMillis();

		XMLGregorianCalendar xmlDate = XmlGregorianCalendarUtil
				.getNowAsXMLGregorianCalendar();

		long tsAfter = System.currentTimeMillis();

		assertTrue(xmlDate.toGregorianCalendar().getTime().getTime() >= tsBefore);
		assertTrue(xmlDate.toGregorianCalendar().getTime().getTime() <= tsAfter);
	}
}
