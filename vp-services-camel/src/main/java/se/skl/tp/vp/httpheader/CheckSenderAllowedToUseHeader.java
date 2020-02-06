package se.skl.tp.vp.httpheader;

public interface CheckSenderAllowedToUseHeader {
  boolean isSenderIdAllowedToUseXrivtaOriginalConsumerIdHeader(String senderId);
}
