package se.skl.tp.vp.httpheader;


import static se.skl.tp.vp.constants.PropertyConstants.SENDER_ID_ALLOWED_LIST;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.util.TestLogAppender;


@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.properties")
public class CheckSenderAllowedToUseHeaderTest {

  @Value("${" + SENDER_ID_ALLOWED_LIST + "}")
  private String allowedUsers;

  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  private static final String LOG_CLASS = "se.skl.tp.vp.httpheader.CheckSenderAllowedToUseHeaderImpl";

  CheckSenderAllowedToUseHeader checkSenderIdAgainstList;

  @Before
  public void beforeTest() {
    if(checkSenderIdAgainstList==null) {
      checkSenderIdAgainstList = new CheckSenderAllowedToUseHeaderImpl(allowedUsers);
    }
    testLogAppender.clearEvents();
  }

  @Test
  public void senderIdInListTest() {
    Assert.assertTrue(checkSenderIdAgainstList.isSenderIdAllowedToUseXrivtaOriginalConsumerIdHeader("SENDER1"));
    Assert.assertTrue(checkSenderIdAgainstList.isSenderIdAllowedToUseXrivtaOriginalConsumerIdHeader("SENDER2"));
    testLogMessage(2, "Sender 'SENDER1' allowed to set " + HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID);
  }

  @Test
  public void senderIdNotInListTest() {
    Assert.assertFalse(checkSenderIdAgainstList.isSenderIdAllowedToUseXrivtaOriginalConsumerIdHeader("SENDER3"));
    testLogMessage(1, "Sender 'SENDER3' not allowed to set header " + HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID + ", accepted senderId's in '"
        + SENDER_ID_ALLOWED_LIST + "': [" + allowedUsers + "]");
  }

  @Test
  public void listMissingTest() {
    CheckSenderAllowedToUseHeader emptyCheckSenderIdAgainstList = new CheckSenderAllowedToUseHeaderImpl(null);
    Assert.assertFalse(emptyCheckSenderIdAgainstList.isSenderIdAllowedToUseXrivtaOriginalConsumerIdHeader("SENDER2"));
    testLogMessage(1, "The list of approved senders, that can use header " + HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID + " was empty or null. SenderId was SENDER2");
  }

  @Test
  public void senderIDNullTest() {
    Assert.assertFalse(checkSenderIdAgainstList.isSenderIdAllowedToUseXrivtaOriginalConsumerIdHeader(null));
    testLogMessage(1, "The sender was null/empty. Could not check address in list " + SENDER_ID_ALLOWED_LIST +
        ". HTTP header that caused checking: " + HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID + ".");
  }

  @Test
  public void senderIDEmptyTest() {
    Assert.assertFalse(checkSenderIdAgainstList.isSenderIdAllowedToUseXrivtaOriginalConsumerIdHeader(""));
    testLogMessage(1, "The sender was null/empty. Could not check address in list " + SENDER_ID_ALLOWED_LIST +
        ". HTTP header that caused checking: " + HttpHeaders.X_RIVTA_ORIGINAL_SERVICE_CONSUMER_HSA_ID + ".");
  }

  private void testLogMessage(int num, String message) {
    Assert.assertEquals(num, testLogAppender.getNumEvents(LOG_CLASS));
    Assert.assertEquals(message, testLogAppender.getEventMessage(LOG_CLASS, 0));
  }

}
