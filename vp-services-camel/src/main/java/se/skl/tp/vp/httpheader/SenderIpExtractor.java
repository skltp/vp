package se.skl.tp.vp.httpheader;

import org.apache.camel.Message;

public interface SenderIpExtractor {

  String getSenderIpAdress(Message message);

  String getForwardedForAddress(Message message);

  String getCallerRemoteAddress(Message message);

  String getForwardForHeaderName();

  String getCallerRemoteAddressHeaderName();

  Boolean isProxyUsed(Message message);
}
