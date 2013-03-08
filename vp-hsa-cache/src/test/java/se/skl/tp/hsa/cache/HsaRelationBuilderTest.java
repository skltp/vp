package se.skl.tp.hsa.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

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
		Map<Dn, HsaNode> cache = new HashMap<Dn, HsaNode>();
		for(int i = 0; i < dn.length ; i++) {
			cache.put(dn[i], createHsaNode(dn[i].toString(),hsaId[i]));
		}
		HsaRelationBuilder builder = new HsaRelationBuilder(-1);
		Map<String, HsaNode> r = builder.setRelations(cache);
		
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
}
