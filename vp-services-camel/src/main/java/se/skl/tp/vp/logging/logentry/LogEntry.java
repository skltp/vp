package se.skl.tp.vp.logging.logentry;

import java.util.Map;
import lombok.Data;

@Data
public class LogEntry {

  protected LogMetadataInfoType metadataInfo;
  protected LogRuntimeInfoType runtimeInfo;
  protected LogMessageType messageInfo;
  protected String payload;
  protected Map<String, String> extraInfo;

}
