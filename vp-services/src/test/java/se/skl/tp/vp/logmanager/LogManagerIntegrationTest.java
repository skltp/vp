package se.skl.tp.vp.logmanager;

 

import java.io.FileNotFoundException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import javax.sql.DataSource;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transport.email.MailProperties;
import org.mule.transport.sftp.SftpConnector;

import org.soitoolkit.commons.mule.jdbc.JdbcScriptEngine;
import org.soitoolkit.commons.mule.test.AbstractJmsTestUtil;
 
import org.soitoolkit.commons.mule.test.AbstractTestCase;
 
import org.soitoolkit.commons.mule.test.ActiveMqJmsTestUtil;
import org.soitoolkit.commons.mule.test.Dispatcher;
import org.soitoolkit.commons.mule.util.MiscUtil;
import org.soitoolkit.commons.mule.util.MuleUtil;
import org.soitoolkit.commons.mule.sftp.SftpUtil;
import org.soitoolkit.commons.mule.file.FileUtil;
import org.soitoolkit.commons.mule.mail.MailUtil;
import org.soitoolkit.commons.mule.jdbc.JdbcUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 
public class LogManagerIntegrationTest extends AbstractTestCase {
 
	
	private static final Logger log = LoggerFactory.getLogger(LogManagerIntegrationTest.class);
	private static final ResourceBundle rb = ResourceBundle.getBundle("vp-config");
 

 

 
 
 

	private static final String IN_QUEUE         = rb.getString("LOGMANAGER_IN_QUEUE");
	private static final String DEADLETTER_QUEUE = rb.getString("LOGMANAGER_DL_QUEUE");
 

 

	private static final String ERROR_LOG_QUEUE = "SOITOOLKIT.LOG.ERROR";
	private AbstractJmsTestUtil jmsUtil = null;
	
 
	
	/**
	 *
     * DLQ tests expects the following setup in activemq.xml (in the <policyEntry> - element):
     *                   <deadLetterStrategy>
     *                     <!--
     *                      Use the prefix 'DLQ.' for the destination name, and make
     *                      the DLQ a queue rather than a topic
     *                     -->
     *                     <individualDeadLetterStrategy queuePrefix="DLQ." useQueueForQueueMessages="true" />
     *                   </deadLetterStrategy>
     * 
	 */
    public LogManagerIntegrationTest() {
    	
 
    	// TODO: Should this be placed in the baseclass?
    	
    	// Only start up Mule once to make the tests run faster...
    	// Set to false if tests interfere with each other when Mule is started only once.
        setDisposeManagerPerSuite(true);

		// Increase the default 120 sec so that you have a chance to debug...
        setTestTimeoutSecs(120);
    }

	protected String getConfigResources() {
		return "soitoolkit-mule-jms-connector-activemq-embedded.xml," + 

        "soitoolkit-mule-jdbc-datasource-hsql-embedded.xml," +
		"vp-jdbc-connector.xml," +
  
		"vp-common.xml," +
		"services/LogManager-service.xml," +
		"teststub-services/LogManager-teststub-service.xml";
    }

    @Override
	protected void doSetUp() throws Exception {
		super.doSetUp();

		doSetUpJms();

		doSetUpDb();
 

 

 
 
 

 

 
		
 
    }

	private void doSetUpJms() {
		// TODO: Fix lazy init of JMS connection et al so that we can create jmsutil in the declaration
		// (The embedded ActiveMQ queue manager is not yet started by Mule when jmsutil is delcared...)
		if (jmsUtil == null) jmsUtil = new ActiveMqJmsTestUtil();
		

		// Clear queues used for the inbound endpoint
		jmsUtil.clearQueues(IN_QUEUE, DEADLETTER_QUEUE);
 
		
 

		// Clear queues used for error handling
		jmsUtil.clearQueues(ERROR_LOG_QUEUE);
    }
		

	private void doSetUpDb() throws FileNotFoundException {
		DataSource ds = JdbcUtil.lookupDataSource(muleContext, "soitoolkit-jdbc-datasource");
		JdbcScriptEngine se = new JdbcScriptEngine(ds);
		
		try {
			se.execute("src/environment/setup/vp-db-drop-tables.sql");
		} catch (Throwable ex) {
			log.warn("Drop db script failed, maybe no db exists? " + ex.getMessage());
		}
		se.execute("src/environment/setup/vp-db-create-tables.sql");
		se.execute("src/environment/setup/vp-db-insert-testdata.sql");
    }
 

    public void testLogManager_ok() throws JMSException {

		Map<String, String> props = new HashMap<String, String>();
    	final  String inputFile   = "src/test/resources/testfiles/LogManager-input.txt";
    	String expectedResultFile = "src/test/resources/testfiles/LogManager-expected-result.txt";
        String receivingService   = "LogManager-teststub-service";

		final  int timeout        = 5000;
 
		String input          = MiscUtil.readFileAsString(inputFile);
		String expectedResult = MiscUtil.readFileAsString(expectedResultFile);


		// Setup inbound endpoint for jms
		String inboundEndpoint = "jms://" + IN_QUEUE;



 
		// Invoke the service and wait for the transformed message to arrive at the receiving teststub service
		MuleMessage reply = dispatchAndWaitForServiceComponent(inboundEndpoint, input, props, receivingService, timeout);
 


		Map<String, String> map = (Map<String, String>)reply.getPayload();
		assertEquals(2, map.size());
		
		String outId = map.get("ID"); 
		String outValue = map.get("VALUE"); 
		String transformedMessage = outId + "=" + outValue;

		// Verify the result, i.e. the transformed message
        assertEquals(expectedResult, transformedMessage);


		// Verify inbound jms-queues
        assertEquals(0, jmsUtil.browseMessagesOnQueue(IN_QUEUE).size());
        assertEquals(0, jmsUtil.browseMessagesOnQueue(DEADLETTER_QUEUE).size());
 

 

		// Verify error-queue
        assertEquals(0, jmsUtil.browseMessagesOnQueue(ERROR_LOG_QUEUE).size());
    }

    
}
