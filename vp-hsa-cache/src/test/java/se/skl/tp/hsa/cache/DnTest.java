package se.skl.tp.hsa.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;

import org.junit.Test;

public class DnTest {
	
	@Test
	public void testEquals() throws Exception {
		Dn dn1 = new Dn("ou=Nässjö VC DLM,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE");	
		Dn dn2 = new Dn("ou=Nässjö VC DLM,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE");		
		Dn dn3 = new Dn("ou=Nässjö VC DLK,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE");		
		
		assertFalse(dn1.equals(null));
		assertFalse(dn1.equals("Anything"));
		assertFalse(dn1.equals(dn3));
		assertEquals(dn1,dn1);
		assertEquals(dn1,dn2);
	}

	@Test
	public void testStringIntern() throws Exception {
		Dn dn1 = new Dn("ou=Nässjö VC DLM,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE");	
		Dn dn2 = new Dn("ou=Nässjö VC DLM,ou=Nässjö Primärvårdsområde,ou=Höglandets sjukvårdsområde,o=Landstinget i Jönköping,l=VpW,c=SE");		

		Field field = Dn.class.getDeclaredField("parts");
		
		assertNotNull(field);
		field.setAccessible(true);
		
		String [] parts1 = (String[])field.get(dn1);
		String [] parts2 = (String[])field.get(dn2);
		
		assertSame(parts1[0], parts2[0]);
		assertSame(parts1[1], parts2[1]);
		assertSame(parts1[2], parts2[2]);
		assertSame(parts1[3], parts2[3]);
		assertSame(parts1[4], parts2[4]);
		assertSame(parts1[5], parts2[5]);
	}
}
