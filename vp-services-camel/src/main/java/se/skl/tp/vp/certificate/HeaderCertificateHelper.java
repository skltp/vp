package se.skl.tp.vp.certificate;

import org.apache.camel.Message;

public interface HeaderCertificateHelper {
    String getSenderIDFromHeaderCertificate(Object certificate);

    String getSenderIDFromHeader(Message header);
}
