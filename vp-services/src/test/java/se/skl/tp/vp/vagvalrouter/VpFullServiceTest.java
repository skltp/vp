/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.vp.vagvalrouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.junit.BeforeClass;
import org.junit.Test;
import org.soitoolkit.commons.mule.test.AbstractJmsTestUtil;
import org.soitoolkit.commons.mule.test.ActiveMqJmsTestUtil;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;

import se.skl.tp.vp.util.HttpHeaders;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;
import se.skl.tp.vp.vagvalrouter.producer.VpTestProducerLogger;
import se.skltp.domain.subdomain.getproducdetail.v1.Product;

public class VpFullServiceTest extends AbstractTestCase {

	private static final int    CLIENT_TIMEOUT_MS = 600000000;
	private static final String PRODUCT_ID = "SW123";
	private static final String TJANSTE_ADRESS = "https://localhost:20000/vp/tjanst1";
	private static final String TJANSTE_ADRESS_SHORT_TIMEOUT  = "https://localhost:20000/vp/tjanst1-short-timeout";
	private static final String TJANSTE_ADRESS_LONG_TIMEOUT   = "https://localhost:20000/vp/tjanst1-long-timeout";
	private static final String LOGICAL_ADDRESS               = "vp-test-producer";
	private static final String LOGICAL_ADDRESS_HTTP               = "vp-test-producer_http";
	private static final String LOGICAL_ADDRESS_NOT_FOUND     = "unknown-logical-address";
	private static final String LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_BEFORE     = " unknown-logical-address";
	private static final String LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_AFTER     = "unknown-logical-address ";
	private static final String LOGICAL_ADDRESS_NO_CONNECTION = "vp-test-producer-no-connection";
	private static final String LOGICAL_ADDRESS_ZERO_LENGTH = "zeroLength";
	private static final String LOGICAL_ADDRESS_500 = "response500";

    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("vp-config","vp-config-override");

	private VpFullServiceTestConsumer_MuleClient testConsumer = null;

	private int normal_timeout_ms = 0;
	private int short_timeout_ms = 0;
	private int long_timeout_ms = 0;

	private static final String LOG_INFO_QUEUE = rb.getString("SOITOOLKIT_LOG_INFO_QUEUE");
	private static final String LOG_ERROR_QUEUE = rb.getString("SOITOOLKIT_LOG_ERROR_QUEUE");
	private AbstractJmsTestUtil jmsUtil = null;
	
	static SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
	
	public VpFullServiceTest() {
		super();
		
		// Only start up Mule once to make the tests run faster...
		// Set to false if tests interfere with each other when Mule is started
		// only once.
		//setDisposeContextPerClass(true);
		
		normal_timeout_ms = Integer.parseInt(rb.getString("TEST_NORMAL_TIMEOUT_MS"));
		short_timeout_ms = Integer.parseInt(rb.getString("TEST_SHORT_TIMEOUT_MS"));
		long_timeout_ms = Integer.parseInt(rb.getString("TEST_LONG_TIMEOUT_MS"));
	}
	
	@Override
	protected String getConfigResources() {
		return 
			"soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
			"vp-common.xml," +
			"services/VagvalRouter-service.xml," +
			"vp-teststubs-and-services-config.xml";
	}
	
	@BeforeClass
	public static void setupTjanstekatalogen() throws Exception {
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();
		vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS,               "https://localhost:19000/vardgivare-b/tjanst1"));
		vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS_HTTP,               "http://localhost:19001/vardgivare-b/tjanst1"));
		vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS_NO_CONNECTION, "https://www.google.com:81"));
        vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS_ZERO_LENGTH,    "http://localhost:8081/zeroLength"));
        vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS_500, "http://localhost:8081/response500"));
		svimi.setVagvalInputs(vagvalInputs);
	}
	
	@Override
	public void doSetUp() throws Exception {

		// TODO: Fix lazy init of JMS connection et al so that we can create jmsutil in the declaration
		// (The embedded ActiveMQ queue manager is not yet started by Mule when jmsutil is delcared...)
		if (jmsUtil == null) jmsUtil = new ActiveMqJmsTestUtil();
		
		// Clear queues used for the logging
		jmsUtil.clearQueues(LOG_INFO_QUEUE);
		jmsUtil.clearQueues(LOG_ERROR_QUEUE);
		
		testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPConsumerConnector", CLIENT_TIMEOUT_MS);
	}

	@Test
	public void testHappyDays() throws Exception {
		
		assertEquals(0, jmsUtil.consumeMessagesOnQueue(LOG_INFO_QUEUE).size());
		
		Product p = testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS);
		assertEquals(PRODUCT_ID, p.getId());
		
		//Verify log messages
		assertEquals(2, jmsUtil.browseMessagesOnQueue(LOG_INFO_QUEUE).size());
		
		List<Message> logMessages = jmsUtil.consumeMessagesOnQueue(LOG_INFO_QUEUE);
		
		TextMessage infoMessageReqIn = (TextMessage)logMessages.get(0);
		assertInfoEventExtraInformation(infoMessageReqIn, "127.0.0.1");
		
		TextMessage infoMessageRespOut = (TextMessage)logMessages.get(1);
		assertInfoEventExtraInformation(infoMessageRespOut, "127.0.0.1");
	}
	
	/**
	 * Test for scenario where a reverse-proxy/loadbalancer sits in front of VP
	 * and is required to forward original request info to VP for:
	 * <ol>
	 * <li>X-Forwarded-Proto</li>
	 * <li>X-Forwarded-Host</li>
	 * <li>X-Forwarded-Port</li>
	 * <li>X-Forwarded-For</li>
	 * </ol>
	 * <p>The information is needed for:
	 * <ul>
	 * <li>re-writing URL's in WSDL's returned for WSDL lookups using ?wsdl</li>
	 * <li>logging/tracing</li>
	 * </ul>
	 */
/*
	VP_HTTP_HEADER_NAME_FORWARDED_PROTO=X-Forwarded-Proto
			VP_HTTP_HEADER_NAME_FORWARDED_HOST=X-Forwarded-Host
			VP_HTTP_HEADER_NAME_FORWARDED_PORT=X-Forwarded-Port

			VAGVALROUTER_SENDER_IP_ADRESS_HTTP_HEADER=X-Forwarded-For
*/
	@Test
	public void testLoadBalancerXForwardedInfo() throws Exception {
		
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(rb.getString("VP_HTTP_HEADER_NAME_FORWARDED_PROTO"), "https");
		properties.put(rb.getString("VP_HTTP_HEADER_NAME_FORWARDED_HOST"), "skltp-lb.example.org");
		properties.put(rb.getString("VP_HTTP_HEADER_NAME_FORWARDED_PORT"), "443");
		properties.put(rb.getString("VAGVALROUTER_SENDER_IP_ADRESS_HTTP_HEADER"), "10.0.0.10");
		assertEquals(0, jmsUtil.consumeMessagesOnQueue(LOG_INFO_QUEUE).size());
		
		Product p = testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS, properties);
		assertEquals(PRODUCT_ID, p.getId());
		
		//Verify log messages
		assertEquals(2, jmsUtil.browseMessagesOnQueue(LOG_INFO_QUEUE).size());
		
		List<Message> logMessages = jmsUtil.consumeMessagesOnQueue(LOG_INFO_QUEUE);
		
		TextMessage infoMessageReqIn = (TextMessage)logMessages.get(0);
		assertInfoEventExtraInformation(infoMessageReqIn, "10.0.0.10");
		assertTrue("X-Forwarded-Proto present in logmessage", infoMessageReqIn.getText().contains("<extraInfo><name>httpXForwardedProto</name><value>https</value></extraInfo>"));
		assertTrue("X-Forwarded-Host present in logmessage", infoMessageReqIn.getText().contains("<extraInfo><name>httpXForwardedHost</name><value>skltp-lb.example.org</value></extraInfo>"));
		assertTrue("X-Forwarded-Port present in logmessage", infoMessageReqIn.getText().contains("<extraInfo><name>httpXForwardedPort</name><value>443</value></extraInfo>"));
		
		
		TextMessage infoMessageRespOut = (TextMessage)logMessages.get(1);
		assertInfoEventExtraInformation(infoMessageRespOut, "10.0.0.10");
	}

	private void assertInfoEventExtraInformation(TextMessage infoMessage, String expectedSenderIpAdress)
			throws JMSException {
		assertTrue(infoMessage.getText().contains("<extraInfo><name>senderIpAdress</name><value>" + expectedSenderIpAdress + "</value></extraInfo>"));
    	assertTrue(infoMessage.getText().contains("<extraInfo><name>receiverid</name><value>vp-test-producer</value></extraInfo>"));
    	assertTrue(infoMessage.getText().contains("<extraInfo><name>senderid</name><value>tp</value>"));
    	assertTrue(infoMessage.getText().contains("<extraInfo><name>wsdl_namespace</name><value>urn:riv:domain:subdomain:GetProductDetail:1:rivtabp20</value></extraInfo>"));
    	assertTrue(infoMessage.getText().contains("<extraInfo><name>servicecontract_namespace</name><value>urn:riv:domain:subdomain:GetProductDetailResponder:1</value></extraInfo>"));
    	assertTrue(infoMessage.getText().contains("<extraInfo><name>rivversion</name><value>RIVTABP20</value></extraInfo>"));
	}

	
	/**
	 * Verify that we handle a response from service producer with a 0 length response.
	 * @throws Exception
	 */
	@Test
	public void testZeroLengthResponseMessage() throws Exception {
		
    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_ZERO_LENGTH);
    		fail("Expected error here!");
    	} catch (Exception ex) {
			assertTrue(ex.getMessage().contains("No content found!"));
    	}
	}
	
	/**
	 * Verify that we handle a response from service producer with http.status=500 and a payload
	 * (which is typically non-xml).
	 * @throws Exception
	 */
	@Test
	public void test500ResponseMessage() throws Exception {
		
    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_500);
    		fail("Expected error here!");
    	} catch (Exception ex) {
			assertTrue(ex.getMessage().contains("Invalid content found!"));
    	}
	}
	
	/**
	 * Verify that VP send info log events to JMS queue by default
	 * @throws Exception
	 */
	@Test
	public void testThatInfoEventsAreLoggedToJmsQueue() throws Exception {
		
		assertEquals(0, jmsUtil.consumeMessagesOnQueue(LOG_INFO_QUEUE).size());

		Product p = testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS);
		assertEquals(PRODUCT_ID, p.getId());

		assertEquals("Wrong number of messages on jms queue " + LOG_INFO_QUEUE, 2, jmsUtil.consumeMessagesOnQueue(LOG_INFO_QUEUE).size());
	}
	
	/**
	 * Verify that VP send error log events to JMS queue by default
	 * @throws Exception
	 */
	@Test
	public void testThatErrorEventsAreLoggedToJmsQueue() throws Exception {
		
		assertEquals(0, jmsUtil.consumeMessagesOnQueue(LOG_ERROR_QUEUE).size());
		
    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND);
    		fail("Expected error here!");
    	} catch (Exception ex) {
    		// TODO: handle exception
    	}
    	assertEquals("Wrong number of messages on jms queue " + LOG_ERROR_QUEUE, 1, jmsUtil.consumeMessagesOnQueue(LOG_ERROR_QUEUE).size());
	}
	
	/**
	 * Verify that VP send error log events to JMS queue by default
	 * @throws Exception
	 */
	@Test
	public void testErrorLogsContainsExtraInformation() throws Exception {
		
		assertEquals(0, jmsUtil.consumeMessagesOnQueue(LOG_ERROR_QUEUE).size());
		
    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND);
    		fail("Expected error here!");
    	} catch (Exception ex) {
    		// Expected failure, proceed with assertions
    	}
    
    	TextMessage errorMessage = (TextMessage)jmsUtil.consumeMessagesOnQueue(LOG_ERROR_QUEUE).get(0);
    		
    	assertTrue(errorMessage.getText().contains("<extraInfo><name>receiverid</name><value>unknown-logical-address</value></extraInfo>"));
    	assertTrue(errorMessage.getText().contains("<extraInfo><name>senderid</name><value>tp</value>"));
    	assertTrue(errorMessage.getText().contains("<extraInfo><name>wsdl_namespace</name><value>urn:riv:domain:subdomain:GetProductDetail:1:rivtabp20</value></extraInfo>"));
    	assertTrue(errorMessage.getText().contains("<extraInfo><name>servicecontract_namespace</name><value>urn:riv:domain:subdomain:GetProductDetailResponder:1</value></extraInfo>"));
    	assertTrue(errorMessage.getText().contains("<extraInfo><name>rivversion</name><value>RIVTABP20</value></extraInfo>"));
    	assertTrue(errorMessage.getText().contains("<extraInfo><name>errorCode</name><value>VP004</value></extraInfo>"));
	}
	
	/**
	 * Verify that VP send error log events to JMS queue by default
	 * @throws Exception
	 */
	@Test
	public void testErrorLogsCorrelatesToRequest() throws Exception {
		
		assertEquals(0, jmsUtil.consumeMessagesOnQueue(LOG_ERROR_QUEUE).size());
		
    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND);
    		fail("Expected error here!");
    	} catch (Exception ex) {
    		// Expected failure, proceed with assertions
    	}
    	  	
    	//Verify INFO end ERROR message correlates to each other
    	TextMessage errorMessage = (TextMessage)jmsUtil.consumeMessagesOnQueue(LOG_ERROR_QUEUE).get(0);
    	TextMessage infoMessage = (TextMessage)jmsUtil.consumeMessagesOnQueue(LOG_INFO_QUEUE).get(0);
    	assertBusinessCorrelationId(errorMessage, infoMessage);
	}
	
	/**
	 * Verify that VP send error log events to JMS queue by default
	 * @throws Exception
	 */
	@Test
	public void testWhenErrorOneInfoEventAndOneErrorEventIsCreated() throws Exception {
		
		assertEquals(0, jmsUtil.consumeMessagesOnQueue(LOG_ERROR_QUEUE).size());
		
    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND);
    		fail("Expected error here!");
    	} catch (Exception ex) {
    		// Expected failure, proceed with assertions
    	}
    	
    	//Verify one INFO event is logged for the request and one ERROR event is logged for the error
    	assertEquals("Wrong number of messages on jms queue " + LOG_ERROR_QUEUE, 1, jmsUtil.browseMessagesOnQueue(LOG_ERROR_QUEUE).size());
    	assertEquals("Wrong number of messages on jms queue " + LOG_INFO_QUEUE, 1, jmsUtil.browseMessagesOnQueue(LOG_INFO_QUEUE).size());
	}

	private void assertBusinessCorrelationId(TextMessage errorMessage, TextMessage infoMessage)
			throws JMSException {
		
		int errCorrIdStart = errorMessage.getText().indexOf("<businessCorrelationId>") + "<businessCorrelationId>".length();
    	int errCorrIdEnd = errorMessage.getText().indexOf("</businessCorrelationId>");
    	
    	int infoCorrIdStart = infoMessage.getText().indexOf("<businessCorrelationId>") + "<businessCorrelationId>".length();
    	int infoCorrIdEnd = infoMessage.getText().indexOf("</businessCorrelationId>");
    	
    	String errorBusinessCorrelationId = errorMessage.getText().substring(errCorrIdStart, errCorrIdEnd);
    	String infoBusinessCorrelationId = infoMessage.getText().substring(infoCorrIdStart, infoCorrIdEnd);
    	
    	assertNotNull(errorBusinessCorrelationId);
    	assertNotNull(infoBusinessCorrelationId);
    	assertEquals(errorBusinessCorrelationId, infoBusinessCorrelationId);
	}

	@Test
	public void testShortConnectionTimeout() throws Exception {
		
		long ts = System.currentTimeMillis();
		try {
			testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_SHORT_TIMEOUT, LOGICAL_ADDRESS_NO_CONNECTION);
			fail("An timeout should have occurred");
		} catch (Throwable e) {
			ts = System.currentTimeMillis() - ts;
			System.out.println(">>>>>>>>>>>>>>>>>>" + e.getMessage());
			assertTrue("Expected time to be between short_timeout_ms (" + short_timeout_ms + ") and normal_timeout_ms (" + normal_timeout_ms + ") but was " + ts + " ms.", short_timeout_ms < ts && ts < normal_timeout_ms);
			assertTrue(e.getMessage().contains("VP009 Error connecting to service producer at address https://www.google.com:81"));
		}
	}

	@Test
	public void testVP009IsThrownWhenLongConnectionTimeout() throws Exception {
		
		long ts = System.currentTimeMillis();
		try {
			testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_LONG_TIMEOUT, LOGICAL_ADDRESS_NO_CONNECTION);
			fail("An timeout should have occurred");
		} catch (Throwable ex) {
			ts = System.currentTimeMillis() - ts;
			// NOTE: this test is really tricky to make predictable - requires trying to
			// connect to a port that typically doesn't respond at all, rather just drops
			// packages - like a "stealth" mode firewall port.
			// Not an easy (or even possible?) thing to do in Java.
			//assertTrue("Expected time to be longer than long_timeout_ms (" + long_timeout_ms + ") but was " + ts + " ms.", ts > long_timeout_ms);
			assertTrue(ex.getMessage().contains("VP009 Error connecting to service producer at address https://www.google.com:81"));
		}
	}
	
	@Test
	public void testMandatoryPropertiesArePropagatedToExternalProducer() throws Exception {
		
		Map<String, String> properties = new HashMap<String, String>();

    	testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS, properties);
		
		assertEquals("tp", VpTestProducerLogger.getLatestRivtaOriginalSenderId());
		assertEquals("SKLTP VP/2.0", VpTestProducerLogger.getLatestUserAgent());
	}
	
	@Test
	public void testValidVpInstanceIdToExternalProducer() throws Exception {
		final String THIS_VP_INSTANCE_ID = rb.getString("VP_INSTANCE_ID");		
		final String CONSUMERS_SENDER_ID_IN_CERT = "tp";
		
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(HttpHeaders.X_VP_INSTANCE_ID, THIS_VP_INSTANCE_ID);
		properties.put(HttpHeaders.X_VP_SENDER_ID, CONSUMERS_SENDER_ID_IN_CERT);

    	testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS, properties);
		
    	//externt https connection it should not propagate
		assertEquals(null, VpTestProducerLogger.getLatestSenderId());
		assertEquals(null, VpTestProducerLogger.getLatestVpInstanceId());
	}
	
	@Test
	public void testValidVpInstanceIdToInternalProducer() throws Exception {
		final String THIS_VP_INSTANCE_ID = rb.getString("VP_INSTANCE_ID");		
		final String CONSUMERS_SENDER_ID_IN_CERT = "tp";
		
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(HttpHeaders.X_VP_INSTANCE_ID, THIS_VP_INSTANCE_ID);
		properties.put(HttpHeaders.X_VP_SENDER_ID, CONSUMERS_SENDER_ID_IN_CERT);

    	testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_HTTP, properties);
		
		assertEquals(CONSUMERS_SENDER_ID_IN_CERT, VpTestProducerLogger.getLatestSenderId());
		assertEquals(THIS_VP_INSTANCE_ID, VpTestProducerLogger.getLatestVpInstanceId());
	}
	
	@Test
	public void testVP007IsThrownWhenNotAuthorizedConsumerIsProvided() throws Exception {
	
		final String NOT_AUHTORIZED_CONSUMER_HSAID = "UNKNOWN_CONSUMER";
		final String THIS_VP_INSTANCE_ID = rb.getString("VP_INSTANCE_ID");
		
		/*
		 * Provide a valid vp instance id to trigger check if provided http header x-vp-sender-id
		 * is a authorized consumer, otherwise sender id is extracted from certificate.
		 */
 		Map<String, String> properties = new HashMap<String, String>();
    	properties.put(HttpHeaders.X_VP_SENDER_ID, NOT_AUHTORIZED_CONSUMER_HSAID);
    	properties.put(HttpHeaders.X_VP_INSTANCE_ID, THIS_VP_INSTANCE_ID);

    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS, properties);
    		fail("Expected error here!");
    	} catch (Exception ex) {
    		assertTrue(ex.getMessage().contains("VP007 Authorization missing for serviceNamespace: urn:riv:domain:subdomain:GetProductDetailResponder:1, receiverId: vp-test-producer, senderId: " + NOT_AUHTORIZED_CONSUMER_HSAID));
    	}
	}
	
	//TODO: Lägg till test för VP001, VP002, VP003, VP005 och VP006
	
	@Test
	public void testVP004IsThrownWhenNoLogicalAddressIsFound() throws Exception {
		
		Map<String, String> properties = new HashMap<String, String>();
    	
    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND, properties);
    		fail("Expected error here!");
    	} catch (Exception ex) {
    		assertTrue(ex.getMessage().contains("VP004 No receiverId (logical address) found for serviceNamespace:urn:riv:domain:subdomain:GetProductDetailResponder:1, receiverId:" + LOGICAL_ADDRESS_NOT_FOUND));
    	}
	}

	@Test
	public void testVP004IsThrownWhenNoLogicalAddressIsFoundAndWhitespaceBefore() throws Exception {
		
		final String THIS_VP_INSTANCE_ID = rb.getString("VP_INSTANCE_ID");		
		Map<String, String> properties = new HashMap<String, String>();
    	
    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_BEFORE, properties);
    		fail("Expected error here!");
    	} catch (Exception ex) {
    		assertTrue(ex.getMessage().contains("VP004 No receiverId (logical address) found for serviceNamespace:urn:riv:domain:subdomain:GetProductDetailResponder:1, receiverId:" + LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_BEFORE + ", RivVersion:RIVTABP20, From:" + THIS_VP_INSTANCE_ID + ". Whitespace detected in incoming request!"));    	}
	}

	@Test
	public void testVP004IsThrownWhenNoLogicalAddressIsFoundAndWhitespaceAfter() throws Exception {

		final String THIS_VP_INSTANCE_ID = rb.getString("VP_INSTANCE_ID");		

 		Map<String, String> properties = new HashMap<String, String>();
    	
    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_AFTER, properties);
    		fail("Expected error here!");
    	} catch (Exception ex) {
    		String msg = ex.getMessage();
    		assertTrue(msg.contains("VP004 No receiverId (logical address) found for serviceNamespace:urn:riv:domain:subdomain:GetProductDetailResponder:1, receiverId:" + LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_AFTER + ", RivVersion:RIVTABP20, From:" + THIS_VP_INSTANCE_ID + ". Whitespace detected in incoming request!"));
    	}
	}

	@Test
	public void testWhenProducerReturnsFaultItsPropagatedCorrectToConsumer() throws Exception {
		
		final String AUHTORIZED_CONSUMER_HSAID = "tp";
		final String PRODUCT_ID_EXCEPTION = "Exception";
		final String THIS_VP_INSTANCE_ID = rb.getString("VP_INSTANCE_ID");		

 		Map<String, String> properties = new HashMap<String, String>();
    	properties.put(HttpHeaders.X_VP_SENDER_ID, AUHTORIZED_CONSUMER_HSAID);
    	properties.put(HttpHeaders.X_VP_INSTANCE_ID, THIS_VP_INSTANCE_ID);

    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID_EXCEPTION, TJANSTE_ADRESS, LOGICAL_ADDRESS, properties);
    		fail("Expected error here!");
    	} catch (Exception ex) {
    		assertTrue(ex.getMessage().contains("<faultcode>soap:Server</faultcode>"));
    		assertTrue(ex.getMessage().contains("<faultstring>PP01 Product Does Not Exist</faultstring>"));
    	}
	}
	
	private static VagvalMockInputRecord createVagvalRecord(String receiverId, String adress) {
		VagvalMockInputRecord vagvalInput = new VagvalMockInputRecord();
		vagvalInput.receiverId = receiverId;
		vagvalInput.senderId = "tp";
		vagvalInput.rivVersion = "RIVTABP20";
		vagvalInput.serviceContractNamespace = "urn:riv:domain:subdomain:GetProductDetailResponder:1";
		vagvalInput.adress = adress;
		return vagvalInput;
	}
}