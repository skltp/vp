package se.skl.tp.vp.errorhandling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;

@Service
public class ExceptionUtil {

  VpCodeMessages vpCodeMessages;

  @Autowired
  public ExceptionUtil(VpCodeMessages vpCodeMessages) {
    this.vpCodeMessages = vpCodeMessages;
  }

  public VpSemanticException createVpSemanticException(VpSemanticErrorCodeEnum codeEnum){
    return createVpSemanticException(codeEnum, null);
  }

  public VpSemanticException createVpSemanticException(VpSemanticErrorCodeEnum codeEnum, Object ...suffix){
    String errorMsg = createMessage(codeEnum);
    String messageDetails = createDetailsMessage(codeEnum, suffix);

    return new VpSemanticException(codeEnum, errorMsg, messageDetails);
  }

  public String createMessage(VpSemanticErrorCodeEnum codeEnum) {
    return codeEnum + " " + vpCodeMessages.getMessage(codeEnum);  //NTP-1944 todo: add platform name
  }

  public String createDetailsMessage(VpSemanticErrorCodeEnum codeEnum, Object ...suffix) {
    String errorMsg = vpCodeMessages.getMessageDetails(codeEnum);
    return String.format(errorMsg, suffix);
  }
}
