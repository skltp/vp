package se.skl.tp.vp.logging.logentry;

import lombok.Data;

@Data
public class LogMessageType {
  protected String message;
  protected LogMessageExceptionType exception;
}
