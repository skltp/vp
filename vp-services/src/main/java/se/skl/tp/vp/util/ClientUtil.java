package se.skl.tp.vp.util;

import java.net.MalformedURLException;
import java.net.URL;

public class ClientUtil {
    
    /**
     * 
     * @param adressOfWsdl, e.g. http://localhost:8080/tppoc-vagvalsinfo-module-web-g/services/SokVagvalsInfoService?wsdl
     * @return
     */
	public static URL createEndpointUrlFromWsdl(String adressOfWsdl) {
		try {
			return new URL(adressOfWsdl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}	

	/**
	 * 
	 * @param serviceAddress, e.g. http://localhost:8080/tppoc-vagvalsinfo-module-web-g/services/SokVagvalsInfoService
	 * @return
	 */
	public static URL createEndpointUrlFromServiceAddress(String serviceAddress) {
		return createEndpointUrlFromWsdl(serviceAddress + "?wsdl");
	}	

}
