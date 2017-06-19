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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by martul on 2017-06-15.
 */
public class VP002FelkodeTest extends VPFelkoderTest {

    @BeforeClass
    public static void setupBeforeClass() {
        // override value in property file before injecting into spring context
        System.setProperty("VAGVALROUTER_SENDERID", "obefintlig");
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
