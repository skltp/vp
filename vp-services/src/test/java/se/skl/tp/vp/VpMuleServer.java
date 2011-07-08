package se.skl.tp.vp;

 
import org.soitoolkit.commons.mule.test.StandaloneMuleServer;
 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VpMuleServer {

	private static final Logger logger = LoggerFactory.getLogger(VpMuleServer.class);

	public static final String MULE_SERVER_ID   = "vp";
 
//	public static final String MULE_CONFIG      = "vp-teststubs-and-services-config.xml"; // both teststubs and services
//	public static final String MULE_CONFIG      = "vp-teststubs-only-config.xml"; // only teststubs
	public static final String MULE_CONFIG      = "vp-config.xml"; // only services

	public static void main(String[] args) throws Exception {
		
 
		StandaloneMuleServer muleServer = new StandaloneMuleServer(MULE_SERVER_ID, MULE_CONFIG);
 
		muleServer.run();
	}
	
 
}