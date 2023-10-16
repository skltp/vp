package se.skl.tp.vp.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import se.skl.tp.vp.errorhandling.VpCodeMessages;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;

@CamelSpringBootTest
@SpringBootTest
public class VpCodeMessagesTest {

  @Autowired
  VpCodeMessages vpCodeMessages;

  @Test
  public void messageByKeyTest() throws Exception {
    String result = vpCodeMessages.getMessage("VP001");
    assertEquals("Rivta-version saknas i anrop eller stöds ej av tjänsteplattformen.", result);
  }

  @Test
  public void messageByErrorCodeTest() throws Exception {
    String result = vpCodeMessages.getMessage(VpSemanticErrorCodeEnum.VP001);
    assertEquals("Rivta-version saknas i anrop eller stöds ej av tjänsteplattformen.", result);
  }

  @Test
  public void messageDetailsByKeyTest() throws Exception {
    String result = vpCodeMessages.getMessageDetails("VP001");
    assertEquals("No RIV version configured", result);
  }

  @Test
  public void messageDetailsByErrorCodeTest() throws Exception {
    String result = vpCodeMessages.getMessageDetails(VpSemanticErrorCodeEnum.VP001);
    assertEquals("No RIV version configured", result);
  }

  @Test
  public void defaultMessageTest() throws Exception {
    String getMessageForDefault = vpCodeMessages.getMessage(VpSemanticErrorCodeEnum.getDefault());
    String staticDefaultMessage = VpCodeMessages.getDefaultMessage();
    assertEquals(getMessageForDefault, staticDefaultMessage);
  }

  @Configuration
  @ComponentScan(basePackages = {"se.skl.tp.vp.errorhandling"})
  static class VpCodeMessagesTestConfiguration {

  }
}