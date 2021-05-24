package se.skl.tp.vp.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@CamelSpringBootTest
@SpringBootTest(classes = HttpHeaderFilterProperties.class)
public class HttpHeaderFilterPropertiesTest {

  @Autowired
  private HttpHeaderFilterProperties headerFilter;

  private List<String> expectedHeaderRemovals = new ArrayList(
      Arrays.asList(
          "X-Forwarded-For",
          "PEER_CERTIFICATES",
          "X-MULE_CORRELATION_GROUP_SIZE",
          "MULE_CORRELATION_ID"
      ));

  private List<String> expectedHeaderKeepers = new ArrayList(
      Arrays.asList(
          "x-vp-sender-id",
          "x-vp-instance-id"
      ));


  @Test
  public void testDefaultRemoveRegExp() {
    String regExp = headerFilter.getRequestHeadersToRemove();
    for (String header : expectedHeaderRemovals) {
      assertTrue(header.matches(regExp));
    }
  }

  @Test
  public void testDefaultKeepRegExp() {
    String regExp = headerFilter.getRequestHeadersToKeep();
    for (String header : expectedHeaderKeepers) {
      assertTrue(header.matches(regExp));
    }
  }

}
