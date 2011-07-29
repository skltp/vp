package se.skl.tp.vp.getsupportedservicecontracts;

import java.util.List;
import java.util.Map;

import javax.jws.WebService;
import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import se.riv.itintegration.registry.getsupportedservicecontracts.v1.rivtabp21.GetSupportedServiceContractsResponderInterface;
import se.riv.itintegration.registry.getsupportedservicecontractsresponder.v1.GetSupportedServiceContractsResponseType;
import se.riv.itintegration.registry.getsupportedservicecontractsresponder.v1.GetSupportedServiceContractsType;
import se.riv.itintegration.registry.v1.ServiceContractType;

@WebService(
		serviceName = "GetSupportedServiceContractsResponderService", 
		endpointInterface="se.riv.itintegration.registry.getsupportedservicecontracts.v1.rivtabp21.GetSupportedServiceContractsResponderInterface", 
		portName = "GetSupportedServiceContractsResponderPort", 
		targetNamespace = "urn:riv:itintegration:registry:GetSupportedServiceContracts:1:rivtabp21",
		wsdlLocation = "schemas/interactions/GetSupportedServiceContractsInteraction/GetSupportedServiceContractsInteraction_1.0_RIVTABP20.wsdl")
public class GetSupportedServiceContractsImpl implements GetSupportedServiceContractsResponderInterface {

	private SimpleJdbcTemplate jdbcTemplate;
	
	public void setDataSource(final DataSource dataSource) {
		this.jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	@Override
	public GetSupportedServiceContractsResponseType getSupportedServiceContracts(
			String logicalAddress, GetSupportedServiceContractsType parameters) {
		
		final GetSupportedServiceContractsResponseType response = new GetSupportedServiceContractsResponseType();
		
		/*
		 * Find out which contracts that are currently supported by
		 * making a query to the service directory
		 */
		final List<Map<String, Object>> contracts = this.jdbcTemplate.queryForList("select * from Tjanstekontrakt as tk order by tk.namnrymd asc");
		for (final Map<String, Object> row : contracts) {
			final Object namespace = row.get("namnrymd");
			
			final ServiceContractType sct = new ServiceContractType();
			sct.setServiceContractNamespace(namespace.toString());
			
			response.getServiceContractNamespaces().add(sct);
		}
		
		return response;
	}
}
