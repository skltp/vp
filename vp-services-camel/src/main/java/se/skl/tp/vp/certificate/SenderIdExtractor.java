package se.skl.tp.vp.certificate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SenderIdExtractor {

  private Pattern certificateSenderIDPattern;

  public SenderIdExtractor(String certificateSenderIDPattern) {
    this.certificateSenderIDPattern = Pattern.compile(certificateSenderIDPattern);
  }

  public String extractSenderFromPrincipal(String principalName) {
    final Matcher matcher = certificateSenderIDPattern.matcher(principalName);

    if (matcher.find()) {
      final String senderId = matcher.group(1);

      log.debug("Found sender id: {}", senderId);
      return SenderIdExtractor.convertIfHexFormat(senderId);
    }
    return null;
  }

  private static String convertIfHexFormat(final String senderId){
    return senderId.startsWith("#") ? convertFromHexToString(senderId.substring(5)) : senderId;
  }

  private static String convertFromHexToString(final String hexString) {
    byte[] txtInByte = new byte[hexString.length() / 2];
    int j = 0;
    for (int i = 0; i < hexString.length(); i += 2) {
      txtInByte[j++] = Byte.parseByte(hexString.substring(i, i + 2), 16);
    }
    return new String(txtInByte);
  }

}
