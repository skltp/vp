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
