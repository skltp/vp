package se.skl.tp.vp.vagvalagent;

import java.util.List;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;
import se.skltp.takcache.exceptions.TakServiceException;
import se.skltp.takcache.services.TakService;

public class TakServiceMock implements TakService {

  private List<VirtualiseringsInfoType> virtualiseringsInfo;
  private List<AnropsBehorighetsInfoType> anropsBehorighetsInfo;

  @Override
  public List<AnropsBehorighetsInfoType> getBehorigheter() throws TakServiceException {
    return anropsBehorighetsInfo;
  }

  @Override
  public List<VirtualiseringsInfoType> getVirtualiseringar() throws TakServiceException {
    return virtualiseringsInfo;
  }

  public void setAnropsBehorighetsInfo(
      List<AnropsBehorighetsInfoType> anropsBehorighetsInfo) {
    this.anropsBehorighetsInfo = anropsBehorighetsInfo;
  }

  public void setVirtualiseringsInfo(
      List<VirtualiseringsInfoType> virtualiseringsInfo) {
    this.virtualiseringsInfo = virtualiseringsInfo;
  }
}
