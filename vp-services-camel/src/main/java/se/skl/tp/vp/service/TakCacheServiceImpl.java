package se.skl.tp.vp.service;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.skl.tp.behorighet.BehorighetHandler;
import se.skl.tp.behorighet.BehorighetHandlerImpl;
import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.vagval.VagvalHandler;
import se.skl.tp.vagval.VagvalHandlerImpl;
import se.skl.tp.vp.config.DefaultRoutingProperties;
import se.skltp.takcache.RoutingInfo;
import se.skltp.takcache.TakCache;
import se.skltp.takcache.TakCacheLog;
import se.skltp.takcache.TakCacheLog.RefreshStatus;

@Service
public class TakCacheServiceImpl implements TakCacheService {

  TakCache takCache;

  BehorighetHandler behorighetHandler;

  VagvalHandler vagvalHandler;

  TakCacheLog takCacheLog = null;

  Date lastResetDate;

  @Autowired
  public TakCacheServiceImpl(HsaCache hsaCache, TakCache takCache, DefaultRoutingProperties defaultRoutingProperties) {
    this.takCache = takCache;
    behorighetHandler = new BehorighetHandlerImpl(hsaCache, takCache, defaultRoutingProperties);
    vagvalHandler = new VagvalHandlerImpl(hsaCache, takCache, defaultRoutingProperties);
  }

  @Override
  public TakCacheLog refresh() {
    takCacheLog = takCache.refresh();
    lastResetDate = new Date();
    return takCacheLog;
  }

  @Override
  public boolean isInitalized() {
    return takCacheLog != null && takCacheLog.getRefreshStatus() != RefreshStatus.REFRESH_FAILED;
  }

  @Override
  public boolean isAuthorized(String senderId, String servicecontractNamespace, String receiverId) {
    return behorighetHandler.isAuthorized(senderId, servicecontractNamespace, receiverId);
  }

  @Override
  public List<RoutingInfo> getRoutingInfo(String tjanstegranssnitt, String receiverAddress) {
    return vagvalHandler.getRoutingInfo(tjanstegranssnitt, receiverAddress);
  }

  @Override
  public Date getLastResetDate() {
    return lastResetDate;
  }

  @Override
  public TakCacheLog getLastRefreshLog(){
    return takCacheLog;
  }


}
