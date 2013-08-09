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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTest {

	@Before
	public void before() {
		System.setProperty("hsaFile1", getClass().getClassLoader().getResource("simpleTestPart1.xml").getFile());
		System.setProperty("hsaFile2", getClass().getClassLoader().getResource("simpleTestPart2.xml").getFile());
	}
	
	public void after() {
		System.clearProperty("hsaFile1");
		System.clearProperty("hsaFile2");
	}
	
	@Test
	public void testSpringContext() throws Exception {	
		ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
		
		
		HsaCache cache = (HsaCache) ctx.getBean("cache");
		assertNotNull(cache);
		String p1 = cache.getParent("SE0000000000-1234");
		String p2 = cache.getParent("SE0000000001-1234");
		
		assertSame(p1,p2);
		
		String p3 = cache.getParent(p1);
		assertEquals("SE0000000003-1234", p3);
		
		String p4 = cache.getParent(p3);
		assertEquals("SE0000000004-1234", p4);
	}
}
