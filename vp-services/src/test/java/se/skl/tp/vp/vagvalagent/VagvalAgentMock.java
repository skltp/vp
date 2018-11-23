package se.skl.tp.vp.vagvalagent;

import java.util.ArrayList;
import java.util.List;
import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.vp.util.MessageProperties;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;
import se.skltp.takcache.TakCacheImpl;
import se.skltp.takcache.services.TakService;

public class VagvalAgentMock extends VagvalAgent {

  private List<VirtualiseringsInfoType> virtualiseringsInfo;
  private List<AnropsBehorighetsInfoType> anropsBehorighetsInfo;

  private se.skltp.takcache.TakCacheImpl takCache;

  public VagvalAgentMock(){
    this(null, "");
  }

  public VagvalAgentMock(TakService takService, HsaCache hsaCache, String delimiter) {
    this.setMessageProperties(MessageProperties.getInstance());

    takCache = new TakCacheImpl(takService);
    takCache.setUseVagvalCache(true);
    takCache.setUseBehorighetCache(true);
    this.init(hsaCache, takCache, delimiter);
  }

  public VagvalAgentMock(HsaCache hsaCache, String delimiter) {
    this.virtualiseringsInfo = new ArrayList<>();
    this.anropsBehorighetsInfo = new ArrayList<>();
    this.setMessageProperties(MessageProperties.getInstance());

    TakServiceMock takServiceMock = new TakServiceMock();
    takServiceMock.setAnropsBehorighetsInfo(anropsBehorighetsInfo);
    takServiceMock.setVirtualiseringsInfo(virtualiseringsInfo);
    takCache = new TakCacheImpl(takServiceMock);
    takCache.setUseVagvalCache(true);
    takCache.setUseBehorighetCache(true);
    this.init(hsaCache, takCache, delimiter);
  }

  public List<VirtualiseringsInfoType> getMockVirtualiseringsInfo() {
    return virtualiseringsInfo;
  }

  public List<AnropsBehorighetsInfoType> getMockAnropsBehorighetsInfo() {
    return anropsBehorighetsInfo;
  }

  public void setLocalCacheFileName(String fileName) {
    takCache.setLocalTakCacheFileName(fileName);
  }

  public void setUseVagvalCache(boolean useVagvalCache) {
    takCache.setUseVagvalCache(useVagvalCache);
  }

  public void setUseBehorighetCache(boolean useBehorighetCache) {
    takCache.setUseBehorighetCache(useBehorighetCache);
  }
}
