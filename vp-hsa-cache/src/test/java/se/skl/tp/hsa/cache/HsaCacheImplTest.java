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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.skl.tp.hsa.cache.HsaCache.*;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.AssertTrue;

import org.junit.Test;

public class HsaCacheImplTest {

	@Test
	public void testSimple() throws Exception {
		URL url = getClass().getClassLoader().getResource("simpleTest.xml");

		HsaCache impl = new HsaCacheImpl().init(url.getFile());
				
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000000-1234"));
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000001-1234"));
		assertEquals("SE0000000003-1234", impl.getParent("SE0000000002-1234"));
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000003-1234"));
		assertEquals(DEFAUL_ROOTNODE, impl.getParent("SE0000000004-1234"));
	
		assertEquals(Arrays.asList(new String[]{"SE0000000003-1234"}), impl.getChildren("SE0000000004-1234"));
		assertEquals(Arrays.asList(new String[]{"SE0000000002-1234"}), impl.getChildren("SE0000000003-1234"));
		assertEquals(Arrays.asList(new String[]{"SE0000000000-1234","SE0000000001-1234"}), impl.getChildren("SE0000000002-1234"));
	}
	
	@Test
	public void testSimpleISO88591() throws Exception {
		URL url = getClass().getClassLoader().getResource("simpleTest-ISO-8859-1.xml");

		HsaCache impl = new HsaCacheImpl().init(url.getFile());
				
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000000-1234"));
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000001-1234"));
		assertEquals("SE0000000003-1234", impl.getParent("SE0000000002-1234"));
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000003-1234"));
		assertEquals(DEFAUL_ROOTNODE, impl.getParent("SE0000000004-1234"));
	
		assertEquals(Arrays.asList(new String[]{"SE0000000003-1234"}), impl.getChildren("SE0000000004-1234"));
		assertEquals(Arrays.asList(new String[]{"SE0000000002-1234"}), impl.getChildren("SE0000000003-1234"));
		List<String> children = impl.getChildren("SE0000000002-1234");
		assertTrue(children.size() == 2);
		assertTrue(children.contains("SE0000000000-1234"));
		assertTrue(children.contains("SE0000000001-1234"));
	}
	
	@Test
	public void testReinitialize() throws Exception {
		URL url = getClass().getClassLoader().getResource("simpleTestShort.xml");
		HsaCache impl = new HsaCacheImpl(url.getFile());
		
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000001-1234"));
		
		url = getClass().getClassLoader().getResource("simpleTest.xml");
		impl.init(url.getFile());
		
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000001-1234"));	
	}
	
	@Test
	public void testReinitializeFail() throws Exception {
		URL url = getClass().getClassLoader().getResource("simpleTestShort.xml");
		HsaCache impl = new HsaCacheImpl(url.getFile());
		
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000001-1234"));
		
		try {
			impl.init("dummyfile.txt");
			fail("Expected exception");
		} catch(Exception e) {
			// Expected
		}
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000001-1234"));
	}
	
	@Test(expected=HsaCacheInitializationException.class)
	public void testInvalid() throws Exception {
		new HsaCacheImpl("notfound.xml");
	}
	
	@Test
	public void testNotInitializedGivesDefaultRoot() throws Exception {
		HsaCacheImpl impl = new HsaCacheImpl();
		assertEquals(DEFAUL_ROOTNODE,impl.getParent("jabbadabba"));
	}
	
	@Test
	public void defaultRootNodeReturnedWhenHsaIdNotFoundInCache() throws Exception {
		HsaCacheImpl impl = new HsaCacheImpl();
		URL url = getClass().getClassLoader().getResource("simpleTest.xml");
		impl.init(url.getFile());
		
		impl.getParent("jabbadabba");
	}
}
