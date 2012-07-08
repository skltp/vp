package se.skl.tp.vp.deployer;

import org.junit.Before;
import org.junit.Test;

//
public class DeployerMainTest {

	private DeployerMain dm;
	
	@Before
	public void before() {
		dm = new DeployerMain();
	}
	
	@Test
	public void deploy() throws Exception {
		dm.deploy("src/test/resources/test.jar", true);
	}
	
	@Test
	public void parseJar() throws Exception {
	}
}
