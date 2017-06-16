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
