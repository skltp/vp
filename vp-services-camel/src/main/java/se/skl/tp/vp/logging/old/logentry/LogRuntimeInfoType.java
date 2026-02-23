package se.skl.tp.vp.logging.old.logentry;

import lombok.Data;

@Data
public class LogRuntimeInfoType {
  protected String componentId;
  protected String messageId;
  protected String businessCorrelationId;

}
