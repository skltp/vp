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
import se.skl.tp.vp.config.HsaLookupBehorighetProperties;
import se.skl.tp.vp.config.HsaLookupVagvalProperties;
import se.skltp.takcache.*;
import se.skltp.takcache.TakCacheLog.RefreshStatus;

@Service
public class TakCacheServiceImpl implements TakCacheService {
  TakCache takCache;
  BehorighetHandler behorighetHandler;
  VagvalHandler vagvalHandler;
  TakCacheLog takCacheLog = null;
  Date lastResetDate;

  /**
   * Constructor.
   * This constructor has been extended with two new parameters, <code>BehorigheterCache</code> and <code>VagvalCache</code>.
   * If <code>takCache</code> has a <code>BehorigheterCache</code> and <code>VagvalCache</code> set already, those are
   * used to create the corresponding <code>Handlers</code>. Otherwise, the incoming caches are used instead.
   * @param hsaCache
   * @param takCache
   * @param behorigheterCache
   * @param vagvalCache
   * @param defaultRoutingProperties
   */
  @Autowired
  public TakCacheServiceImpl(HsaCache hsaCache, TakCache takCache, BehorigheterCache behorigheterCache, VagvalCache vagvalCache, DefaultRoutingProperties defaultRoutingProperties,
                             HsaLookupBehorighetProperties hsaLookupBehorighetProperties, HsaLookupVagvalProperties hsaLookupVagvalProperties) {
    this.takCache = takCache;

    // The below if-then-else clauses are used to select a suitable cache depending on if we're executing unit or integration tests, or are in "production".
    BehorigheterCache behorigheterCacheToSet = (takCache.getBehorigeterCache() != null ? takCache.getBehorigeterCache() : behorigheterCache);
    VagvalCache vagvalCacheToSet = (takCache.getVagvalCache() != null ? takCache.getVagvalCache() : vagvalCache);

    behorighetHandler = new BehorighetHandlerImpl(hsaCache, behorigheterCacheToSet, defaultRoutingProperties, hsaLookupBehorighetProperties);
    vagvalHandler = new VagvalHandlerImpl(hsaCache, vagvalCacheToSet, defaultRoutingProperties, hsaLookupVagvalProperties);
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
