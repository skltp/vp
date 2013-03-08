package se.skl.tp.hsa.cache;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import org.junit.Test;

public class HsaNodePrinterTest {
	
	String expected = 
			"dn=o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000004-1234,lineNo=33"+ System.getProperty("line.separator") +
			"  dn=ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000003-1234,lineNo=24"+ System.getProperty("line.separator") +
			"    dn=ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000002-1234,lineNo=18"+ System.getProperty("line.separator") +
			"      dn=ou=N\u00e4ssj\u00f6 VC DLK,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000000-1234,lineNo=12"+ System.getProperty("line.separator") +
			"      dn=ou=N\u00e4ssj\u00f6 VC DLM,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000001-1234,lineNo=6"+ System.getProperty("line.separator");

	
	@Test
	public void testPrint() throws Exception {
		URL url = getClass().getClassLoader().getResource("simpleTest.xml");
		HsaCacheImpl impl = (HsaCacheImpl)new HsaCacheFactoryImpl().create(url.getFile(), "UTF-8");
		
		HsaNode topNode = impl.getNode("SE0000000004-1234");
		
		StringWriter sw = new StringWriter();
		
		new HsaNodePrinter(topNode,2).printTree(new PrintWriter(sw));
		
		assertEquals(expected, sw.toString());
		
	}
}
