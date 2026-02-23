package se.skl.tp.vp.logging.old.logentry;

import lombok.Data;

@Data
public class LogMessageExceptionType {
  protected String exceptionClass;
  protected String exceptionMessage;
  protected String stackTrace;


}
