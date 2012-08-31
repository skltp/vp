package se.skl.tp.vp;

 
import java.util.ArrayList;
import java.util.List;

import org.soitoolkit.commons.mule.test.StandaloneMuleServer;

import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;


public class VpMuleServer {


	public static final String MULE_SERVER_ID   = "vp";
 
	public static final String MULE_CONFIG      = "vp-teststubs-and-services-config.xml"; // both teststubs and services
//	public static final String MULE_CONFIG      = "vp-teststubs-only-config.xml"; // only teststubs
//	public static final String MULE_CONFIG      = "vp-config.xml"; // only services

	public static void main(String[] args) throws Exception {
 
		initTk();
		
		StandaloneMuleServer muleServer = new StandaloneMuleServer(MULE_SERVER_ID, MULE_CONFIG, true);
 
		muleServer.run();
	}
	
	static private void initTk() {
		// NOTE this test user the same certificates for consumer,
		// virtualisation-plattform and producer
		// The certs are located in certs folder and has SERIALNUMBER=tp

		// Initialize the vagvalsinfo that is supposed to be in Tjanstekatalogen
		// when the call
		// to the virtual service is made
		// Note certificate serial number is used as sender
		SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		VagvalMockInputRecord vi = new VagvalMockInputRecord();
		vi.receiverId = "vp-test-producer";
		vi.senderId = "tp";
		vi.rivVersion = "RIVTABP20";
		vi.serviceNamespace = "urn:skl:tjanst1:rivtabp20";
		vi.adress = "https://localhost:19000/vardgivare-b/tjanst1";
		vagvalInputs.add(vi);
		svimi.setVagvalInputs(vagvalInputs);
	}
}