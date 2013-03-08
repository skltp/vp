package se.skl.tp.hsa.cache;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.Test;

public class HsaCacheImplTest {

	@Test
	public void testSimple() throws Exception {
		URL url = getClass().getClassLoader().getResource("simpleTest.xml");

		HsaCache impl = new HsaCacheFactoryImpl().create(url.getFile(), "UTF-8");
				
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000000-1234"));
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000001-1234"));
		assertEquals("SE0000000003-1234", impl.getParent("SE0000000002-1234"));
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000003-1234"));
		assertEquals(null, impl.getParent("SE0000000004-1234"));
	}
	
	@Test(expected=HsaCacheInitializationException.class)
	public void testInvalid() throws Exception {
		HsaCacheImpl impl = new HsaCacheImpl();
		impl.init("notfound.xml", "UTF-8");
	}
	
	@Test(expected=HsaCacheInitializationException.class)
	public void testNotInitialized() throws Exception {
		HsaCacheImpl impl = new HsaCacheImpl();
		impl.getParent("jabbadabba");
	}
	
	@Test(expected=HsaCacheNodeNotFoundException.class)
	public void testNodeNotFound() throws Exception {
		HsaCacheImpl impl = new HsaCacheImpl();
		URL url = getClass().getClassLoader().getResource("simpleTest.xml");
		impl.init(url.getFile(), "UTF-8");
		
		impl.getParent("jabbadabba");
	}
}
