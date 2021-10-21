package se.skl.tp.vp.errorhandling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.PropertyConstants;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;

@Service
public class ExceptionUtil {

  VpCodeMessages vpCodeMessages;

  @Value("${vp.instance.name}")
  String platformName;

  @Autowired
  public ExceptionUtil(VpCodeMessages vpCodeMessages) {
    this.vpCodeMessages = vpCodeMessages;
  }

  public VpSemanticException createVpSemanticException(VpSemanticErrorCodeEnum codeEnum){
    return createVpSemanticException(codeEnum, "");
  }

  public VpSemanticException createVpSemanticException(VpSemanticErrorCodeEnum codeEnum, Object ...suffix){
    String errorMsg = createMessage(codeEnum);
    String messageDetails = createDetailsMessage(codeEnum, suffix);

    return new VpSemanticException(codeEnum, errorMsg, messageDetails);
  }

  public String createMessage(VpSemanticErrorCodeEnum codeEnum) {
    return codeEnum + " [" + platformName + "] "+ vpCodeMessages.getMessage(codeEnum);
  }

  public String createDetailsMessage(VpSemanticErrorCodeEnum codeEnum, Object ...suffix) {
    String errorMsg = vpCodeMessages.getMessageDetails(codeEnum);
    return String.format(errorMsg, suffix);
  }
}
