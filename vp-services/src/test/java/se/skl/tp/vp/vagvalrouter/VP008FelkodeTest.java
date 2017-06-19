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

import org.junit.Test;
import org.soitoolkit.commons.mule.test.junit4.AbstractTestCase;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tjanstekatalogen stub är tom
 */
public class VP008FelkodeTest  extends AbstractTestCase {
    private VpFullServiceTestConsumer_MuleClient testConsumer = null;
    private static final int    CLIENT_TIMEOUT_MS = 600000000;
    private static final String PRODUCT_ID = "SW123";

    private static final String TJANSTE_ADRESS =                                      "https://localhost:20000/vp/tjanst1";
    private static final String LOGICAL_ADDRESS =                                    "vp-test-producer";

    @Override
    protected String getConfigResources() {
        return
                "soitoolkit-mule-jms-connector-activemq-embedded.xml," +
                        "vp-common.xml," +
                        "services/VagvalRouter-service.xml," +
                        "vp-teststubs-and-services-config.xml";
    }

    @Override
    public void doSetUp() throws Exception {
        testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPConsumerConnector", CLIENT_TIMEOUT_MS);
    }
    @Test
    public void testVP008() throws Exception {
        try {
            testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, LOGICAL_ADDRESS);
            fail("Expected error here!");
        } catch (Throwable ex) {
            assertTrue(ex.getMessage().contains("VP008 No contact with Tjanstekatalogen at startup, and no local cache to fallback on, not possible to route call"));
        }
    }

}
