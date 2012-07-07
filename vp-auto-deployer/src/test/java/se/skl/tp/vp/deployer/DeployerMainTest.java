package se.skl.tp.vp.deployer;

import java.io.File;

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
	public void init() throws Exception {
		dm.open(new File("test-out.jar"));
	}
	
	@Test
	public void parseJar() throws Exception {
		dm.open(new File("test-out.jar"));
		dm.prepare("src/test/resources/test.jar");
		dm.close();
	}
}
