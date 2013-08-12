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
package se.skl.tp.hsa.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

/**
 * Class representing a DN (Distinguished Name)
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
public class Dn {
	
	/**
	 * Parts of the DN
	 */
	private final String [] parts;
			
	/**
	 * Creates a ned Dn instance from a text String
	 * 
	 * @param dn dn as a string
	 */
	Dn(String dn) {
		StringTokenizer tok = new StringTokenizer(dn, ",");
		List<String> parts = new ArrayList<String>(10);
		while(tok.hasMoreTokens()) {
			parts.add(tok.nextToken().trim().intern());
		}
		this.parts = parts.toArray(new String[]{});
	}
	
	/**
	 * Create a new Dn instance from parts
	 * 
	 * @param parts
	 */
	private Dn(String [] parts) {
		this.parts = parts;
	}
	
	/**
	 * Returns the parent Dn for this Dn.
	 * 
	 * @return parent dn
	 */
	public Dn parentDn(){		
		return (parts.length == 1) ?
				null:
				new Dn(Arrays.copyOfRange(parts, 1, parts.length));
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtils.join(parts, ",");
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(parts);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Dn other = (Dn) obj;
		if (!Arrays.equals(parts, other.parts))
			return false;
		return true;
	}
	
}	
