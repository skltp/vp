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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;

import org.junit.Test;

public class HsaFileValidatorTest {

	@Test
	public void testName() throws Exception {		
		URL url = getClass().getClassLoader().getResource("invalidTest.xml");
		String [] args = new String[]{"-f", url.getFile(), "-w", "2", "-o", "target/output.txt"};
		
		HsaFileValidator.main(args);
		
		BufferedReader br = new BufferedReader(new FileReader("target/output.txt"));
		
		String line1, line2 = "";
		try {
			line1 = br.readLine();
			line2 = br.readLine();
		} finally {
			br.close();
		}
		
		System.out.println(line1);
		System.out.println(line2);
		
		assertTrue(line1.startsWith("ERROR HsaObject entry invalid @ LineNo:33, entry: dn=ou="));
		assertTrue(line2.startsWith("WARNING: No parent for HSA-ID=SE0000000004-1234"));
	}
}
