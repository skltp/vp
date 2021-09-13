package se.skl.tp.vp.httpheader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import se.skl.tp.vp.util.TestLogAppender;

@CamelSpringBootTest
public class IPWhitelistHandlerTest {

  IPWhitelistHandler ipWhitelistHandler;
  TestLogAppender testLogAppender = TestLogAppender.getInstance();

  private static final String LOG_CLASS = "se.skl.tp.vp.httpheader.IPWhitelistHandlerImpl";

  private static final String whitelist = "127.0.0.1,1.2.3.4,5.6.7.8";

  @BeforeEach
  public void beforeTest(){
    if(ipWhitelistHandler==null){
      ipWhitelistHandler = new IPWhitelistHandlerImpl(whitelist);
    }

    testLogAppender.clearEvents();
  }

  @Test
  public void ipInWhitelistTest() {
    assertTrue(ipWhitelistHandler.isCallerOnWhiteList("1.2.3.4"));
    assertTrue(ipWhitelistHandler.isCallerOnWhiteList("5.6.7.8"));
  }

  @Test
  public void ipNotInWhitelistTest() {
    assertFalse(ipWhitelistHandler.isCallerOnWhiteList("127.0.0.2"));
    testLogMessage(1, "Caller was not on the white list of accepted IP-addresses. IP-address: 127.0.0.2, accepted IP-addresses in IP_WHITE_LIST:[" +
            whitelist + "]");
  }

  @Test
  public void whitelistMissingTest() {
    IPWhitelistHandler emptyIpWhitelistHandler = new IPWhitelistHandlerImpl(null);
    assertFalse(emptyIpWhitelistHandler.isCallerOnWhiteList("1.2.3.4"));
    testLogMessage(1, "A check against the ip address whitelist was requested, but the whitelist is configured empty");
  }

  @Test
  public void whitelistEmptyTest() {
    IPWhitelistHandler emptyIpWhitelistHandler = new IPWhitelistHandlerImpl("");
    assertFalse(emptyIpWhitelistHandler.isCallerOnWhiteList("1.2.3.4"));
    testLogMessage(1, "A check against the ip address whitelist was requested, but the whitelist is configured empty");
  }

  @Test
  public void senderIpAndWhitelistNullShouldReturnFalseTest() {
    IPWhitelistHandler emptyIpWhitelistHandler = new IPWhitelistHandlerImpl(null);
    assertFalse(emptyIpWhitelistHandler.isCallerOnWhiteList(null));
  }

  @Test
  public void senderIpAndWhitelistEmptyShouldReturnFalseTest() {
    IPWhitelistHandler emptyIpWhitelistHandler = new IPWhitelistHandlerImpl("");
    assertFalse(emptyIpWhitelistHandler.isCallerOnWhiteList(""));
  }

  @Test
  public void senderIpNullTest() {
    assertFalse(ipWhitelistHandler.isCallerOnWhiteList(null));
    testLogMessage(1, "A potential empty ip address from the caller, ip adress is: null.");
  }

  @Test
  public void senderIpEmptyTest() {
    assertFalse(ipWhitelistHandler.isCallerOnWhiteList(""));
    testLogMessage(1, "A potential empty ip address from the caller, ip adress is: .");
  }


  @Test
  public void isCallerOnWhiteListMatchesSubdomain(){

    String whiteListOfSubDomains = "127.0.0,127.0.1.0";
    IPWhitelistHandlerImpl ipWhitelistHandler = new IPWhitelistHandlerImpl(whiteListOfSubDomains);
    assertTrue(ipWhitelistHandler.isCallerOnWhiteList("127.0.0.1"));
  }

  @Test
  public void isCallerOnWhiteListDoesNotMatchSubdomain(){

    String whiteListOfSubDomains = "127.0.0,127.0.1";
    IPWhitelistHandlerImpl ipWhitelistHandler = new IPWhitelistHandlerImpl(whiteListOfSubDomains);
    assertFalse(ipWhitelistHandler.isCallerOnWhiteList("127.0.2.1"));
  }

  @Test
  public void isCallerOnWhiteListOkWhenWhiteListContainsLeadingWhiteSpaces(){

    final String whiteListWithWhiteSpaces ="127.0.0.1, 127.0.0.2";
    IPWhitelistHandlerImpl ipWhitelistHandler = new IPWhitelistHandlerImpl(whiteListWithWhiteSpaces);
    assertTrue(ipWhitelistHandler.isCallerOnWhiteList("127.0.0.2"));
  }


  private void testLogMessage(int num, String message) {
    String logClass = IPWhitelistHandlerImpl.class.getName();
    assertEquals(num, testLogAppender.getNumEvents(logClass));
    assertTrue(testLogAppender.getEventMessage(logClass, 0).contains(message));
  }
}