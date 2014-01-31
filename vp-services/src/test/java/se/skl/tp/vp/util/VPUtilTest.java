package se.skl.tp.vp.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class VPUtilTest {

	@Test
	public void extractIpAdressFromRemoteClientAddress() {
		String remoteClientAddress = "/127.0.0.1:52440";
		String ipAddress = VPUtil.extractIpAddress(remoteClientAddress );
		assertEquals("127.0.0.1", ipAddress);
	}

}
