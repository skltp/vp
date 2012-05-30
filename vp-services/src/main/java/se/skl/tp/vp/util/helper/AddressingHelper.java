package se.skl.tp.vp.util.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.mule.api.MuleMessage;

import se.skl.tp.vagval.wsdl.v1.VisaVagvalRequest;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalResponse;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.util.helper.cert.CertificateExtractor;
import se.skl.tp.vp.util.helper.cert.CertificateExtractorFactory;
import se.skl.tp.vp.vagvalrouter.VagvalInput;

/**
 * Helper class for working with addressing
 * 
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 */
public class AddressingHelper extends VPHelperSupport {

	private VisaVagvalsInterface agent;

	public AddressingHelper(MuleMessage muleMessage, final VisaVagvalsInterface agent, final Pattern pattern,
			final String whiteList) {
		super(muleMessage, pattern, whiteList);

		this.agent = agent;
	}

	public String getAvailableRivProfile() {

		final VagvalInput input = this.createRequestToServiceDirectory();
		final VisaVagvalResponse response = agent.visaVagval(this.createVisaVagvalRequest(input));

		final List<VirtualiseringsInfoType> virts = this.getAllVirtualizedServices(response, input);

		final Set<String> rivProfiles = new HashSet<String>();
		for (final VirtualiseringsInfoType virt : virts) {
			rivProfiles.add(virt.getRivProfil());
		}

		if (rivProfiles.size() == 0) {
			String errorMessage = ("VP005 No Logical Adress with matching Riv-version found for serviceNamespace :"
					+ input.serviceNamespace + ", receiverId:" + input.receiverId + "RivVersion" + input.rivVersion);
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (rivProfiles.size() > 1) {
			String errorMessage = "VP006 More than one Logical Adress with matching Riv-version found for serviceNamespace:"
					+ input.serviceNamespace + ", receiverId:" + input.receiverId;
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		return rivProfiles.iterator().next();
	}

	public String getAddress() {
		/*
		 * Create parameters from message
		 */
		final VagvalInput input = this.createRequestToServiceDirectory();
		return this.getAddressFromAgent(input);
	}

	public String getAddressFromAgent(final VagvalInput input) {
		/*
		 * Validate the parameters
		 */
		this.validateRequest(input);

		/*
		 * Create a real request from the parameters
		 */
		final VisaVagvalRequest request = this.createVisaVagvalRequest(input);

		/*
		 * Get virtualized services
		 */
		final List<VirtualiseringsInfoType> services = this.getAllVirtualizedServices(this.agent.visaVagval(request),
				input);

		/*
		 * Grab the address
		 */
		return this.getAddressToVirtualService(services, input);
	}

	private VagvalInput createRequestToServiceDirectory() {
		VagvalInput vagvalInput = new VagvalInput();

		CertificateExtractorFactory certificateExtractorFactory = new CertificateExtractorFactory(
				this.getMuleMessage(), this.getPattern(), this.getWhiteList());

		CertificateExtractor certHelper = certificateExtractorFactory.creaetCertificateExtractor();
		vagvalInput.senderId = certHelper.extractSenderIdFromCertificate();
		this.getMuleMessage().setProperty(VPUtil.SENDER_ID, vagvalInput.senderId);

		vagvalInput.receiverId = (String) this.getMuleMessage().getProperty(VPUtil.RECEIVER_ID);
		vagvalInput.rivVersion = (String) this.getMuleMessage().getProperty(VPUtil.RIV_VERSION);
		vagvalInput.serviceNamespace = VPUtil.extractNamespaceFromService((QName) this.getMuleMessage().getProperty(
				VPUtil.SERVICE_NAMESPACE));

		return vagvalInput;
	}

	private void validateRequest(final VagvalInput request) {
		if (this.getLog().isDebugEnabled()) {
			this.getLog().debug(
					"Calling vagvalAgent with serviceNamespace:" + request.serviceNamespace + ", receiverId:"
							+ request.receiverId + ", senderId: " + request.senderId);
		}
		if (request.rivVersion == null) {
			String errorMessage = ("VP001 No Riv-version configured in tp-virtuell-tjanst-config, transformer-refs with property name 'rivversion' missing");
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		if (request.senderId == null) {
			String errorMessage = ("VP002 No senderId found in Certificate");
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
		if (request.receiverId == null) {
			String errorMessage = ("VP003 No receiverId found in RivHeader");
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}
	}

	private VisaVagvalRequest createVisaVagvalRequest(final VagvalInput input) {
		return this.createVisaVagvalRequest(input.senderId, input.receiverId, input.serviceNamespace);
	}

	private VisaVagvalRequest createVisaVagvalRequest(String senderId, String receiverId, String tjansteGranssnitt) {
		VisaVagvalRequest vvR = new VisaVagvalRequest();
		vvR.setSenderId(senderId);
		vvR.setReceiverId(receiverId);
		vvR.setTjanstegranssnitt(tjansteGranssnitt);

		XMLGregorianCalendar tidPunkt = XmlGregorianCalendarUtil.getNowAsXMLGregorianCalendar();
		vvR.setTidpunkt(tidPunkt);

		return vvR;
	}

	private String getAddressToVirtualService(final List<VirtualiseringsInfoType> services, final VagvalInput request) {
		String adress = null;
		int noOfMatchingAdresses = 0;
		for (VirtualiseringsInfoType vvInfo : services) {
			if (vvInfo.getRivProfil().equals(request.rivVersion)) {
				adress = vvInfo.getAdress();
				noOfMatchingAdresses++;
			}
		}

		if (noOfMatchingAdresses == 0) {
			String errorMessage = ("VP005 No Logical Adress with matching Riv-version found for serviceNamespace :"
					+ request.serviceNamespace + ", receiverId:" + request.receiverId + "RivVersion" + request.rivVersion);
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (noOfMatchingAdresses > 1) {
			String errorMessage = "VP006 More than one Logical Adress with matching Riv-version found for serviceNamespace:"
					+ request.serviceNamespace + ", receiverId:" + request.receiverId;
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		if (adress == null || adress.trim().length() == 0) {
			String errorMessage = ("VP010 Physical Adress field is empty in Service Producer for serviceNamespace :"
					+ request.serviceNamespace + ", receiverId:" + request.receiverId + "RivVersion" + request.rivVersion);
			this.getLog().error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		adress = "cxf:" + adress;

		return adress;
	}

	private List<VirtualiseringsInfoType> getAllVirtualizedServices(final VisaVagvalResponse response,
			final VagvalInput request) {
		List<VirtualiseringsInfoType> virtualiseringar = response.getVirtualiseringsInfo();
		if (this.getLog().isDebugEnabled()) {
			this.getLog().debug("VagvalAgent response count: " + virtualiseringar.size());
			for (VirtualiseringsInfoType vvInfo : virtualiseringar) {
				this.getLog().debug(
						"VagvalAgent response item RivProfil: " + vvInfo.getRivProfil() + ", Address: "
								+ vvInfo.getAdress());
			}
		}

		if (virtualiseringar.size() == 0) {
			String errorMessage = "VP004 No Logical Adress found for serviceNamespace:" + request.serviceNamespace
					+ ", receiverId:" + request.receiverId;
			this.getLog().info(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		return virtualiseringar;
	}
}
