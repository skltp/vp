package se.skl.tp.vp;

 
import org.soitoolkit.commons.mule.test.StandaloneMuleServer;


public class VpMuleServer {


	public static final String MULE_SERVER_ID   = "vp";
 
//	public static final String MULE_CONFIG      = "vp-teststubs-and-services-config.xml"; // both teststubs and services
	public static final String MULE_CONFIG      = "vp-teststubs-only-config.xml"; // only teststubs
//	public static final String MULE_CONFIG      = "vp-config.xml"; // only services

	public static void main(String[] args) throws Exception {
 
		StandaloneMuleServer muleServer = new StandaloneMuleServer(MULE_SERVER_ID, MULE_CONFIG, false);
 
		muleServer.run();
	}
	
 
}