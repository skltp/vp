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

import org.junit.BeforeClass;
import org.junit.Test;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;
import org.soitoolkit.commons.mule.util.RecursiveResourceBundle;
import se.skl.tp.vp.util.HttpHeaders;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by martul on 2017-06-13.
 */
public class VPFelkoderTest extends AbstractTestCase {

    protected VpFullServiceTestConsumer_MuleClient testConsumer = null;
    protected static final int    CLIENT_TIMEOUT_MS = 600000000;
    protected static final String PRODUCT_ID = "SW123";

    protected static final String TJANSTE_ADRESS =                                      "https://localhost:20000/vp/tjanst1";
    private static final String TJANSTE_ADRESS_MED_FEL_CONFIGURERAD_RIV_VERSION =     "https://localhost:20000/vp/tjanst2";
    private static final String TJANSTE_ADRESS_MED_OBEFINLIG_RIV_VERSION =                  "https://localhost:20000/vp/tjanst3";
    private static final String TJANSTE_ADRESS_DUBLICATE =                            "https://localhost:20000/vp/tjanst4";

    private static final String TJANSTE_ADRESS_LONG_TIMEOUT   =                        "https://localhost:20000/vp/tjanst1-long-timeout";

    private static final String LOGICAL_ADDRESS_NOT_FOUND     =                         "unknown-logical-address";
    private static final String LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_BEFORE     =       " unknown-logical-address";
    private static final String LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_AFTER     =        "unknown-logical-address ";

    protected static final String LOGICAL_ADDRESS =                                       "vp-test-producer";
    private static final String LOGICAL_ADDRESS_MED_EMPTY_PHYSIKAL =                                 "vp-test-producer-empty";
    private static final String LOGICAL_ADDRESS_DUBLICATE =                             "vp-test-producer_D";
    private static final String LOGICAL_ADDRESS_NO_CONNECTION =                         "vp-test-producer-no-connection";


    private static final RecursiveResourceBundle rb = new RecursiveResourceBundle("vp-config","vp-config-override");

    static SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();


    @Override
    public void doSetUp() throws Exception {
        testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPConsumerConnector", CLIENT_TIMEOUT_MS);
    }

    @BeforeClass
    public static void setupTjanstekatalogen() throws Exception {
        List<VagvalMockInputRecord> vagvalInputs = new ArrayList<>();
        vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS,                        "https://localhost:19000/vardgivare-b/tjanst1"));
        vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS_DUBLICATE,              "https://localhost:19000/vardgivare-b/tjanst4"));
        vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS_DUBLICATE,              "https://localhost:19000/vardgivare-b/tjanst4"));
        vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS_NO_CONNECTION,          "https://www.google.com:81"));
        vagvalInputs.add(createVagvalRecord(LOGICAL_ADDRESS_MED_EMPTY_PHYSIKAL,                  ""));
        svimi.setVagvalInputs(vagvalInputs);
    }

    @Override
    protected String[] getConfigFiles() {
        return
            new String[]{"soitoolkit-mule-jms-connector-activemq-embedded.xml",
                "vp-common.xml",
                "services/VagvalRouter-service.xml",
                "vp-teststubs-and-services-config.xml"};
    }



        @Test
    public void testVP003ThrownWhenLogicalAddressIsMissing() throws Exception {
        try {
            testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, "");
            fail("Expected error here!");
        } catch (Throwable ex) {
           assertTrue(ex.getMessage().contains("VP003 No receiverId (logical address) found in message header"));
        }
    }


    @Test
    public void testVP004IsThrownWhenNoLogicalAddressIsFound() throws Exception {
           try {
            testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND);
            fail("Expected error here!");
        } catch (Exception ex) {
        	assertTrue(ex.getMessage().contains("VP004 No receiverId (logical address) found for serviceNamespace:urn:riv:domain:subdomain:GetProductDetailResponder:1, receiverId:" + LOGICAL_ADDRESS_NOT_FOUND));        	
        }
    }
    
    @Test
    public void testVP004IsThrownWhenNoLogicalAddressIsFoundAndWhitespaceBefore() throws Exception {
        final String THIS_VP_INSTANCE_ID = rb.getString("VP_INSTANCE_ID");
        Map<String, String> properties = new HashMap<>();

        try {
            testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_BEFORE, properties);
            fail("Expected error here!");
        } catch (Exception ex) {
    		assertTrue(ex.getMessage().contains("VP004 No receiverId (logical address) found for serviceNamespace:urn:riv:domain:subdomain:GetProductDetailResponder:1, receiverId:" + LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_BEFORE + ", RivVersion:RIVTABP20, From:" + THIS_VP_INSTANCE_ID + ". Whitespace detected in incoming request!"));    	
    	}    	
    }

    @Test
    public void testVP004IsThrownWhenNoLogicalAddressIsFoundAndWhitespaceAfter() throws Exception {

        final String THIS_VP_INSTANCE_ID = rb.getString("VP_INSTANCE_ID");

        Map<String, String> properties = new HashMap<>();

        try {
            testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_AFTER, properties);
            fail("Expected error here!");
        } catch (Exception ex) {
    		String msg = ex.getMessage();
    		assertTrue(msg.contains("VP004 No receiverId (logical address) found for serviceNamespace:urn:riv:domain:subdomain:GetProductDetailResponder:1, receiverId:" + LOGICAL_ADDRESS_NOT_FOUND_WHITESPACE_AFTER + ", RivVersion:RIVTABP20, From:" + THIS_VP_INSTANCE_ID + ". Whitespace detected in incoming request!"));
        }
    }


    @Test
    public void testVP005IsThrownWhenRequestWithNonexistentRivVersion() throws Exception {
        try {
            testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_MED_OBEFINLIG_RIV_VERSION, LOGICAL_ADDRESS);
            fail("Expected error here!");
        } catch (Throwable ex) {
            assertTrue(ex.getMessage().contains("VP005 No receiverId (logical address) with matching Riv-version found for serviceNamespace:urn:riv:domain:subdomain:GetProductDetailResponder:1, receiverId:vp-test-producer, RivVersion:RIVTABP25"));
        }
    }
    
    @Test
    public void testVP006IsThrownWhenAreMoreThanOneLogicalAdress() throws Exception {
        try {
            testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS_DUBLICATE, LOGICAL_ADDRESS_DUBLICATE);
            fail("Expected error here!");
        } catch (Throwable ex) {
            assertTrue(ex.getMessage().contains("VP006 More than one receiverId (logical address) with matching Riv-version found"));
        }
    }
    
    @Test
    public void testVP007IsThrownWhenNotAuthorizedConsumerIsProvided() throws Exception {

        final String NOT_AUHTORIZED_CONSUMER_HSAID = "UNKNOWN_CONSUMER";
        final String THIS_VP_INSTANCE_ID = rb.getString("VP_INSTANCE_ID");

		/*
		 * Provide a valid vp instance id to trigger check if provided http header x-vp-sender-id
		 * is a authorized consumer, otherwise sender id is extracted from certificate.
		 */
        Map<String, String> properties = new HashMap<>();
        properties.put(HttpHeaders.X_VP_SENDER_ID, NOT_AUHTORIZED_CONSUMER_HSAID);
        properties.put(HttpHeaders.X_VP_INSTANCE_ID, THIS_VP_INSTANCE_ID);

        try {
            testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS, properties);
            fail("Expected error here!");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("VP007 Authorization missing for serviceNamespace: urn:riv:domain:subdomain:GetProductDetailResponder:1, receiverId: vp-test-producer, senderId: " + NOT_AUHTORIZED_CONSUMER_HSAID));
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

    /**
     * VagvalMockInputRecord for LOGICAL_ADDRESS_MED_EMPTY_PHYSIKAL has no
     * * @throws Exception
     */
    @Test
    public void testVP010IsThrownWhenPhysicalAdressIsEmptyForThisLocalAddress() throws Exception {
       try {
            testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS_MED_EMPTY_PHYSIKAL);
            fail("Expected error here!");
        } catch (Throwable ex) {
            assertTrue(ex.getMessage().contains("VP010 Physical Address field is empty in Service Producer for serviceNamespace"));
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
