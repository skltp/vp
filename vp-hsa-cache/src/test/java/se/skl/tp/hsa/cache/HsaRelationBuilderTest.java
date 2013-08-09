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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class HsaRelationBuilderTest {

	private static Dn [] dn = new Dn []{
		new Dn("o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE"),
		new Dn("ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE"),
		new Dn("ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE"),
		new Dn("ou=N\u00e4ssj\u00f6 VC DLM,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE"),		
		new Dn("ou=N\u00e4ssj\u00f6 VC DLK,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE"),		
	};
	
	private static String [] hsaId = new String [] {
		"SE0000000001-1234",
		"SE0000000002-1234",
		"SE0000000003-1234",
		"SE0000000004-1234",
		"SE0000000005-1234",
	};
	
	@Test
	public void testSetupRelations() throws Exception {
		Map<Dn, HsaNode> nodes = new HashMap<Dn, HsaNode>();
		for(int i = 0; i < dn.length ; i++) {
			nodes.put(dn[i], createHsaNode(dn[i].toString(),hsaId[i]));
		}
		HsaRelationBuilder builder = new HsaRelationBuilder();
		Map<String, HsaNode> r = builder.setRelations(nodes);
		
		HsaNode topNode = r.get(hsaId[0]);
		
		assertEquals(1, topNode.getChildren().size());
		assertSame(r.get(hsaId[1]), topNode.getChildren().get(0));
		assertSame(r.get(hsaId[2]), r.get(hsaId[1]).getChildren().get(0));
		assertSame(r.get(hsaId[3]), r.get(hsaId[2]).getChildren().get(1));
		assertSame(r.get(hsaId[4]), r.get(hsaId[2]).getChildren().get(0));
	}
	
	private HsaNode createHsaNode(String dn, String hsaId) {
		HsaNode hsaNode = new HsaNode(0);
		hsaNode.setDn(dn);
		hsaNode.setHsaId(hsaId);
		return hsaNode;
	}
	
	@Test
	public void testWarningLevelMinusOne() throws Exception {
		Dn dn1 = new Dn("o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE");
		Dn dn2 = new Dn("ou=N\u00e4ssj\u00f6 VC DLK,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE");		
		
		Map<Dn, HsaNode> nodes = new HashMap<Dn, HsaNode>();
		nodes.put(dn1, createHsaNode(dn1.toString(), "HSA-000001"));
		nodes.put(dn1, createHsaNode(dn2.toString(), "HSA-000002"));
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		HsaRelationBuilder builder = new HsaRelationBuilderWithLog(pw, -1);
		
		builder.setRelations(nodes);
		
		assertEquals("", sw.toString());
	}
	
	@Test
	public void testWarningLevelOne() throws Exception {
		Dn dn1 = new Dn("o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE");
		Dn dn2 = new Dn("ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE");		
		
		
		Map<Dn, HsaNode> nodes = new HashMap<Dn, HsaNode>();
		nodes.put(dn1, createHsaNode(dn1.toString(), "HSA-000001"));
		nodes.put(dn1, createHsaNode(dn2.toString(), "HSA-000002"));
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		HsaRelationBuilder builder = new HsaRelationBuilderWithLog(pw, -1);
		
		builder.setRelations(nodes);
		
		assertEquals("", sw.toString());
	}
	
	@Test
	public void testWarningLevelThree() throws Exception {
		Dn dn1 = new Dn("o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE");
		Dn dn2 = new Dn("ou=N\u00e4ssj\u00f6 VC DLK,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE");		
		
		
		Map<Dn, HsaNode> nodes = new HashMap<Dn, HsaNode>();
		nodes.put(dn1, createHsaNode(dn1.toString(), "HSA-000001"));
		nodes.put(dn1, createHsaNode(dn2.toString(), "HSA-000002"));
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		HsaRelationBuilder builder = new HsaRelationBuilderWithLog(pw, 2);
		builder.setRelations(nodes);
		
		assertTrue(sw.toString().startsWith("WARNING: Parent on 3 levels "));
	}

}
