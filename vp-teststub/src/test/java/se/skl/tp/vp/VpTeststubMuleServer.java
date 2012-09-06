package se.skl.tp.vp;

 
import org.soitoolkit.commons.mule.test.StandaloneMuleServer;

import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VpTeststubMuleServer {


	public static final String MULE_SERVER_ID   = "vp-teststub";
 

	private static final Logger logger = LoggerFactory.getLogger(VpTeststubMuleServer.class);
    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("vp-config");

	public static void main(String[] args) throws Exception {

 
        // Configure the mule-server
        StandaloneMuleServer muleServer = new StandaloneMuleServer(MULE_SERVER_ID, null, false);        
 
        // Start the server
		muleServer.run();
	}

    /**
     * Address based on usage of the servlet-transport and a config-property for the URI-part
     * 
     * @param serviceUrlPropertyName
     * @return
     */
    public static String getAddress(String serviceUrlPropertyName) {

        String url = rb.getString(serviceUrlPropertyName);

	    logger.info("URL: {}", url);
    	return url;
 
    }	
}