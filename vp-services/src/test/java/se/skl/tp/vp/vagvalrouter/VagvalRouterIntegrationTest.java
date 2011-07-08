package se.skl.tp.vp.vagvalrouter;

import static se.skl.tp.vp.vagvalrouter.VagvalRouterTestConsumer.DEFAULT_SERVICE_ADDRESS;
import static se.skl.tp.vp.vagvalrouter.VagvalRouterTestProducer.TEST_ID_FAULT_INVALID_ID;
import static se.skl.tp.vp.vagvalrouter.VagvalRouterTestProducer.TEST_ID_FAULT_TIMEOUT;
import static se.skl.tp.vp.vagvalrouter.VagvalRouterTestProducer.TEST_ID_OK;

import java.util.ResourceBundle;

import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.interceptor.Fault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.test.AbstractJmsTestUtil;
import org.soitoolkit.commons.mule.test.AbstractTestCase;
import org.soitoolkit.commons.mule.test.ActiveMqJmsTestUtil;

public class VagvalRouterIntegrationTest extends AbstractTestCase {
	
	private static final Logger log = LoggerFactory.getLogger(VagvalRouterIntegrationTest.class);
	private static final ResourceBundle rb = ResourceBundle.getBundle("vp-config");

	private static final long   SERVICE_TIMOUT_MS = Long.parseLong(rb.getString("SERVICE_TIMEOUT_MS"));
	private static final String EXPECTED_ERR_TIMEOUT_MSG = "Response timed out (" + SERVICE_TIMOUT_MS + "ms) waiting for message response id ";


	private static final String REQUEST_QUEUE   = rb.getString("VAGVALROUTER_REQUEST_QUEUE");
	private static final String RESPONSE_QUEUE  = rb.getString("VAGVALROUTER_RESPONSE_QUEUE");
 
	private static final String ERROR_LOG_QUEUE = "SOITOOLKIT.LOG.ERROR";
	private AbstractJmsTestUtil jmsUtil = null;
	
    public VagvalRouterIntegrationTest() {
    	
    	// Initialize servlet engine in baseclass with proper values
    	super();
    	
    	// Only start up Mule once to make the tests run faster...
    	// Set to false if tests interfere with each other when Mule is started only once.
        setDisposeManagerPerSuite(true);

		// Increase the default 120 sec so that you have a chance to debug...
        setTestTimeoutSecs(120);
        
        
        
    }

	protected String getConfigResources() {
		return "soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
  
		"vp-common.xml," +
		"services/VagvalRouter-service.xml," +
		"teststub-services/VagvalRouter-teststub-service.xml";
    }

    @Override
	protected void doSetUp() throws Exception {
		super.doSetUp();

		doSetUpJms();
    }

	private void doSetUpJms() {
		// TODO: Fix lazy init of JMS connection et al so that we can create jmsutil in the declaration
		// (The embedded ActiveMQ queue manager is not yet started by Mule when jmsutil is delcared...)
		if (jmsUtil == null) jmsUtil = new ActiveMqJmsTestUtil();
		

		// Clear queues used for the outbound endpoint
		jmsUtil.clearQueues(REQUEST_QUEUE);
		jmsUtil.clearQueues(RESPONSE_QUEUE);
 
		// Clear queues used for error handling
		jmsUtil.clearQueues(ERROR_LOG_QUEUE);
    }

    public void test_ok() throws Fault {
    	String id = TEST_ID_OK;
    	VagvalRouterTestConsumer consumer = new VagvalRouterTestConsumer(DEFAULT_SERVICE_ADDRESS);
		//SampleResponse response = consumer.callService(id);
		//assertEquals("Value" + id,  response.getValue());
	}

	public void test_fault_invalidInput() throws Fault {
		try {
	    	String id = TEST_ID_FAULT_INVALID_ID;
	    	VagvalRouterTestConsumer consumer = new VagvalRouterTestConsumer(DEFAULT_SERVICE_ADDRESS);
			//consumer.callService(id);
	        fail("expected fault");
	    } catch (SOAPFaultException e) {
	    	assertEquals("Invalid Id: " + TEST_ID_FAULT_INVALID_ID, e.getMessage());
	    }
	}

	public void test_fault_timeout() throws Fault {
        try {
	    	String id = TEST_ID_FAULT_TIMEOUT;
	    	VagvalRouterTestConsumer consumer = new VagvalRouterTestConsumer(DEFAULT_SERVICE_ADDRESS);
			//consumer.callService(id);
	        fail("expected fault");
        } catch (SOAPFaultException e) {
            assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().startsWith(EXPECTED_ERR_TIMEOUT_MSG));
        }
    }
}
