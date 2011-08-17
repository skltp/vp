package se.skl.tp.vp.pingforconfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jws.WebService;

import se.riv.itintegration.monitoring.v1.PingForConfigurationResponseType;
import se.riv.itintegration.monitoring.v1.PingForConfigurationType;
import se.riv.itintegration.monitoring.v1.rivtabp21.PingForConfigurationResponderInterface;

@WebService(
		serviceName = "PingForConfigurationResponderService", 
		endpointInterface="se.riv.itintegration.monitoring.v1.rivtabp21.PingForConfigurationResponderInterface", 
		portName = "PingForConfigurationResponderPort", 
		targetNamespace = "urn:riv:itintegration:monitoring:PingForConfiguration:1:rivtabp21",
		wsdlLocation = "schemas/interactions/PingForConfigurationInteraction/PingForConfigurationInteraction_1.0_RIVTABP21.wsdl")
public class PingForConfigurationProducerRivTa21 implements PingForConfigurationResponderInterface {

	@Override
	public PingForConfigurationResponseType pingForConfiguration(
			String logicalAddress, PingForConfigurationType parameters) {
		PingForConfigurationResponseType response = new PingForConfigurationResponseType();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
		
		response.setPingDateTime(formatter.format(new Date()));
		response.setVersion("V1.0");
		
		return response;
	}

}
