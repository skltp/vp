package se.skl.tp.vp.vagvalrouter;

import static org.junit.Assert.*;

import org.junit.Test;

public class ExceptionMessageTransformerTest {
	
	final static String CORRECT_FORMATED_SOAP_FAULT = 
			"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
			"  <soapenv:Header/>" + 
			"  <soapenv:Body>" + 
			"    <soap:Fault xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
			"      <faultcode>soap:Server</faultcode>\n" + 
			"      <faultstring>%s</faultstring>\n" +
			"    </soap:Fault>" + 
			"  </soapenv:Body>" + 
			"</soapenv:Envelope>";


	@Test
	public void transformToSoapFault_ok() {
		String cause = "VP004 No Logical Adress found for serviceNamespace:urn:skl:tjanst1:rivtabp20, receiverId:vp-test-producer_kalle";
		String expectedResult = String.format(CORRECT_FORMATED_SOAP_FAULT, cause);
		
		String actualResult = ExceptionMessageTransformer.transformToSoapFault(cause);
		
		assertNotNull(actualResult);
		assertEquals(expectedResult, actualResult);
	}

}
