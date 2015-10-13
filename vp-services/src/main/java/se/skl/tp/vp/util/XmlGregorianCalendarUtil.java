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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class XmlGregorianCalendarUtil {
	private static DatatypeFactory datatypeFactory = getDatatypeFactory();
	
	// a DatatypeFactory is really expensive to create, only do it once
	private static DatatypeFactory getDatatypeFactory() {
		try {
			return DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("Could not create DatatypeFactory", e);
		}
	}

	/**
	 * Creates an XMLGregorianCalendar representing current time.
	 * @return
	 */
	public static final XMLGregorianCalendar getNowAsXMLGregorianCalendar() {
		GregorianCalendar now = (GregorianCalendar) GregorianCalendar.getInstance();
		return datatypeFactory.newXMLGregorianCalendar(now);
	}
	
	public static final XMLGregorianCalendar fromDate(Date date) {
		Calendar theDate = Calendar.getInstance();
		theDate.setTime(date);
		return datatypeFactory.newXMLGregorianCalendar(
				theDate.get(Calendar.YEAR),
				theDate.get(Calendar.MONTH) + 1,
				theDate.get(Calendar.DATE),
				theDate.get(Calendar.HOUR_OF_DAY),
				theDate.get(Calendar.MINUTE),
				theDate.get(Calendar.SECOND),
				theDate.get(Calendar.MILLISECOND),
				DatatypeConstants.FIELD_UNDEFINED);
	}
}
