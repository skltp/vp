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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;

import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by martul on 2017-06-15.
 */
public class VP002FelkodeTest extends AbstractTestCase {

    protected VpFullServiceTestConsumer_MuleClient testConsumer = null;
    protected static final int    CLIENT_TIMEOUT_MS = 600000000;
    protected static final String PRODUCT_ID = "SW123";

    protected static final String TJANSTE_ADRESS =                                      "https://localhost:20000/vp/tjanst1";
    protected static final String LOGICAL_ADDRESS =                                       "vp-test-producer";
    
    @Override
    public void doSetUp() throws Exception {
        testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPConsumerConnector", CLIENT_TIMEOUT_MS);
    }

    @BeforeClass
    public static void setupTjanstekatalogen() throws Exception {
        // override value in property file before injecting into spring context
        System.setProperty("VAGVALROUTER_SENDERID", "obefintlig");
    }

    @Override
    protected String getConfigResources() {
        return
                        "soitoolkit-mule-jms-connector-activemq-embedded.xml," +
                        "vp-common.xml," +
                        "services/VagvalRouter-service.xml," +
                        "vp-teststubs-and-services-config.xml";
    }


    @AfterClass
    public static void tearDownAfterClass() {
        // restore
        System.clearProperty("VAGVALROUTER_SENDERID");
    }

    @Test
    public void testVP002ThrownWhenNoSenderIdAreInCerteficate() throws Exception {

        try {
            testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS);
            fail("Expected error here!");
        } catch (Throwable ex) {
            assertTrue(ex.getMessage().contains("VP002 No senderId found in Certificate"));
        }
    }

}
