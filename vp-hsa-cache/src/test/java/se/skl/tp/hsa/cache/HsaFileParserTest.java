package se.skl.tp.hsa.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Map;

import org.junit.Test;

public class HsaFileParserTest {

	@Test
	public void testParseFile() throws Exception {
		HsaFileParser parser = new HsaFileParser();
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("simpleTest.xml");
		
		Map<Dn, HsaNode> nodes = parser.parse(is, "UTF-8");
		
		assertEquals(5, nodes.size());
		
		HsaNode node1 = nodes.get(new Dn("o=Landstinget i Jönköping,l=VpW,c=SE"));
		HsaNode node2 = nodes.get(new Dn("ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE"));
		HsaNode node3 = nodes.get(new Dn("ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE"));
		HsaNode node4 = nodes.get(new Dn("ou=Nässjö VC DLM,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE"));
		HsaNode node5 = nodes.get(new Dn("ou=Nässjö VC DLK,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE"));
	
		assertNotNull(node1);
		assertNotNull(node2);
		assertNotNull(node3);
		assertNotNull(node4);
		assertNotNull(node5);
		
		assertEquals("SE0000000004-1234", node1.getHsaId());
		assertEquals("SE0000000003-1234", node2.getHsaId());
		assertEquals("SE0000000002-1234", node3.getHsaId());
		assertEquals("SE0000000001-1234", node4.getHsaId());
		assertEquals("SE0000000000-1234", node5.getHsaId());
	}

	@Test
	public void testParseDuplicateFile() throws Exception {
		HsaFileParser parser = new HsaFileParser();
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("duplicateTest.xml");
		
		try {
			parser.parse(is, "UTF-8");
			fail("Expected IllegalStateException");
		} catch(Exception ex) {
			assertEquals("HsaObject entry invalid @ LineNo:12, Duplicate with: dn=ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE,hsaId=SE0000000003-1234,lineNo=6", ex.getMessage());
		}
	}
	
	@Test
	public void testInvalidEntry() throws Exception {
		
		final String [] error = new String[1];
		
		HsaFileParser parser = new HsaFileParser(){
			@Override
			protected void logError(String msg) {
				error[0] = msg;
			}
		};		
		InputStream is = getClass().getClassLoader().getResourceAsStream("invalidTest.xml");
		
		parser.parse(is, "UTF-8");
		
		assertEquals("HsaObject entry invaliddn=ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE,hsaId=null,lineNo=12 @ LineNo:12", error[0]);
		
	}
}
