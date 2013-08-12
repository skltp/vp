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
package se.skl.tp.vp.util;


import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ExecutionTimerTest {

	@Test
	public void single() throws Exception {
		ExecutionTimer.init();
		ExecutionTimer.start("single");
		Thread.sleep(500);
		ExecutionTimer.stop("single");
		assertTrue(ExecutionTimer.getAll().size() == 1);
		assertTrue(ExecutionTimer.getAll().get("single").getElapsed() >= 500);
	}
	
	@Before
	public void before() {
		ExecutionTimer.remove();
	}
	
	@Test
	public void unInitialized() throws Exception {
		ExecutionTimer.start("single");
		Thread.sleep(200);
		ExecutionTimer.stop("single");
		assertNull(ExecutionTimer.getAll());
	}
	
	@Test
	public void multiple() throws Exception {
		ExecutionTimer.init();
		ExecutionTimer.start("first");
		ExecutionTimer.start("second");
		Thread.sleep(200);
		ExecutionTimer.stop("first");
		
		assertTrue(ExecutionTimer.getAll().size() == 2);
		assertEquals(ExecutionTimer.get("second").getElapsed(), -1L);
	}
}
