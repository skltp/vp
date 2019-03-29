package se.skl.tp.vp.vagvalagent;

import java.util.List;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.takcache.RoutingInfo;
import se.skltp.takcache.TakCacheLog;

public interface VagvalAgentInterface {
  public TakCacheLog resetVagvalCache();
  public List<RoutingInfo> visaVagval(VisaVagvalRequest request);
  public int getNumberOfVagval();
  public int getNumberOfBehorigheter();
}
