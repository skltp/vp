package se.skl.tp.vp.errorhandling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;

@Configuration
@PropertySource("classpath:vp-messages.properties")
public class VpCodeMessages {

  Environment env;

  @Autowired
  public VpCodeMessages(Environment env) {
    this.env = env;
  }

  public String getMessage(VpSemanticErrorCodeEnum vpSemanticErrorCodeEnum) {
    return env.getProperty(vpSemanticErrorCodeEnum.getVpDigitErrorCode());
  }

  public String getMessageDetails(VpSemanticErrorCodeEnum vpSemanticErrorCodeEnum) {
    return env.getProperty(vpSemanticErrorCodeEnum.getVpDigitErrorCode() + "_DETAILS");
  }

  public String getMessage(String key) {
    return env.getProperty(key);
  }

  public String getMessageDetails(String key) {
    return env.getProperty(key+ "_DETAILS");
  }

  public static String getDefaultMessage() {
    return "Fel vid kontakt med tjänsteproducenten.";
  }
}
