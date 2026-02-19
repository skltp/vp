package se.skl.tp.vp.logging.old.logentry;

import lombok.Data;

@Data
public class LogMessageType {
  protected String message;
  protected LogMessageExceptionType exception;
}
