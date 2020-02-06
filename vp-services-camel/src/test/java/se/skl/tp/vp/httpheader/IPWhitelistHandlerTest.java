package se.skl.tp.vp.httpheader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import se.skl.tp.vp.util.TestLogAppender;

@RunWith(SpringRunner.class)
public class IPWhitelistHandlerTest {

  IPWhitelistHandler ipWhitelistHandler;
  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  private static final String LOG_CLASS = "se.skl.tp.vp.httpheader.IPWhitelistHandlerImpl";

  private static final String whitelist = "127.0.0.1,1.2.3.4,5.6.7.8";

  @Before
  public void beforeTest(){
    if(ipWhitelistHandler==null){
      ipWhitelistHandler = new IPWhitelistHandlerImpl(whitelist);
    }

    testLogAppender.clearEvents();
  }

  @Test
  public void ipInWhitelistTest() {
    Assert.assertTrue(ipWhitelistHandler.isCallerOnWhiteList("1.2.3.4"));
    Assert.assertTrue(ipWhitelistHandler.isCallerOnWhiteList("5.6.7.8"));
  }

  @Test
  public void ipNotInWhitelistTest() {
    Assert.assertFalse(ipWhitelistHandler.isCallerOnWhiteList("127.0.0.2"));
    testLogMessage(1, "Caller was not on the white list of accepted IP-addresses. IP-address: 127.0.0.2, accepted IP-addresses in IP_WHITE_LIST:[" +
            whitelist + "]");
  }

  @Test
  public void whitelistMissingTest() {
    IPWhitelistHandler emptyIpWhitelistHandler = new IPWhitelistHandlerImpl(null);
    Assert.assertFalse(emptyIpWhitelistHandler.isCallerOnWhiteList("1.2.3.4"));
    testLogMessage(1, "A check against the ip address whitelist was requested, but the whitelist is configured empty");
  }

  @Test
  public void whitelistEmptyTest() {
    IPWhitelistHandler emptyIpWhitelistHandler = new IPWhitelistHandlerImpl("");
    Assert.assertFalse(emptyIpWhitelistHandler.isCallerOnWhiteList("1.2.3.4"));
    testLogMessage(1, "A check against the ip address whitelist was requested, but the whitelist is configured empty");
  }

  @Test
  public void senderIpAndWhitelistNullShouldReturnFalseTest() {
    IPWhitelistHandler emptyIpWhitelistHandler = new IPWhitelistHandlerImpl(null);
    Assert.assertFalse(emptyIpWhitelistHandler.isCallerOnWhiteList(null));
  }

  @Test
  public void senderIpAndWhitelistEmptyShouldReturnFalseTest() {
    IPWhitelistHandler emptyIpWhitelistHandler = new IPWhitelistHandlerImpl("");
    Assert.assertFalse(emptyIpWhitelistHandler.isCallerOnWhiteList(""));
  }

  @Test
  public void senderIpNullTest() {
    Assert.assertFalse(ipWhitelistHandler.isCallerOnWhiteList(null));
    testLogMessage(1, "A potential empty ip address from the caller, ip adress is: null.");
  }

  @Test
  public void senderIpEmptyTest() {
    Assert.assertFalse(ipWhitelistHandler.isCallerOnWhiteList(""));
    testLogMessage(1, "A potential empty ip address from the caller, ip adress is: .");
  }


  @Test
  public void isCallerOnWhiteListMatchesSubdomain(){

    String whiteListOfSubDomains = "127.0.0,127.0.1.0";
    IPWhitelistHandlerImpl ipWhitelistHandler = new IPWhitelistHandlerImpl(whiteListOfSubDomains);
    Assert.assertTrue(ipWhitelistHandler.isCallerOnWhiteList("127.0.0.1"));
  }

  @Test
  public void isCallerOnWhiteListDoesNotMatchSubdomain(){

    String whiteListOfSubDomains = "127.0.0,127.0.1";
    IPWhitelistHandlerImpl ipWhitelistHandler = new IPWhitelistHandlerImpl(whiteListOfSubDomains);
    Assert.assertFalse(ipWhitelistHandler.isCallerOnWhiteList("127.0.2.1"));
  }

  @Test
  public void isCallerOnWhiteListOkWhenWhiteListContainsLeadingWhiteSpaces(){

    final String whiteListWithWhiteSpaces ="127.0.0.1, 127.0.0.2";
    IPWhitelistHandlerImpl ipWhitelistHandler = new IPWhitelistHandlerImpl(whiteListWithWhiteSpaces);
    Assert.assertTrue(ipWhitelistHandler.isCallerOnWhiteList("127.0.0.2"));
  }


  private void testLogMessage(int num, String message) {
    String logClass = IPWhitelistHandlerImpl.class.getName();
    Assert.assertEquals(num, testLogAppender.getNumEvents(logClass));
    Assert.assertTrue(testLogAppender.getEventMessage(logClass, 0).contains(message));
  }
}