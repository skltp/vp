package se.skl.tp.hsa.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class HsaRelationBuilderTest {

	private static Dn [] dn = new Dn []{
		new Dn("o=Landstinget i Jönköping,l=VpW,c=SE"),
		new Dn("ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE"),
		new Dn("ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE"),
		new Dn("ou=Nässjö VC DLM,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE"),		
		new Dn("ou=Nässjö VC DLK,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE"),		
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
		HsaRelationBuilder builder = new HsaRelationBuilder(-1);
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
		Dn dn1 = new Dn("o=Landstinget i Jönköping,l=VpW,c=SE");
		Dn dn2 = new Dn("ou=Nässjö VC DLK,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE");		
		
		final String [] message = new String[1];
		
		Map<Dn, HsaNode> nodes = new HashMap<Dn, HsaNode>();
		nodes.put(dn1, createHsaNode(dn1.toString(), "HSA-000001"));
		nodes.put(dn1, createHsaNode(dn2.toString(), "HSA-000002"));
		
		HsaRelationBuilder builder = new HsaRelationBuilder(-1) {
			protected void logWarning(String msg) {
				message[0] = msg;
			}
		};
		builder.setRelations(nodes);
		assertNull(message[0]);		
	}
	
	@Test
	public void testWarningLevelOne() throws Exception {
		Dn dn1 = new Dn("o=Landstinget i Jönköping,l=VpW,c=SE");
		Dn dn2 = new Dn("ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE");		
		
		final String [] message = new String[1];
		
		Map<Dn, HsaNode> nodes = new HashMap<Dn, HsaNode>();
		nodes.put(dn1, createHsaNode(dn1.toString(), "HSA-000001"));
		nodes.put(dn1, createHsaNode(dn2.toString(), "HSA-000002"));
		
		HsaRelationBuilder builder = new HsaRelationBuilder(1) {
			protected void logWarning(String msg) {
				message[0] = msg;
			}
		};
		builder.setRelations(nodes);
		assertNull(message[0]);
	}
	
	@Test
	public void testWarningLevelThree() throws Exception {
		Dn dn1 = new Dn("o=Landstinget i Jönköping,l=VpW,c=SE");
		Dn dn2 = new Dn("ou=Nässjö VC DLK,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE");		
		
		final String [] message = new String[1];
		
		Map<Dn, HsaNode> nodes = new HashMap<Dn, HsaNode>();
		nodes.put(dn1, createHsaNode(dn1.toString(), "HSA-000001"));
		nodes.put(dn1, createHsaNode(dn2.toString(), "HSA-000002"));
		
		HsaRelationBuilder builder = new HsaRelationBuilder(2) {
			protected void logWarning(String msg) {
				message[0] = msg;
			}
		};
		builder.setRelations(nodes);
		
		assertNotNull(message[0]);
		assertTrue(message[0].startsWith("Parent on 3 levels"));
	}

}
