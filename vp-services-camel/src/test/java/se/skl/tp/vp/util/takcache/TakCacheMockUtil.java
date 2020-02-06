package se.skl.tp.vp.util.takcache;

import se.skltp.takcache.TakCacheLog;
import se.skltp.takcache.TakCacheLog.RefreshStatus;

public class TakCacheMockUtil {

  public static TakCacheLog createTakCacheLogOk() {
    TakCacheLog takCacheLog = new TakCacheLog();
    takCacheLog.setRefreshSuccessful(true);
    takCacheLog.setRefreshStatus(RefreshStatus.REFRESH_OK);
    return takCacheLog;
  }

  public static TakCacheLog createTakCacheLogFailed() {
    TakCacheLog takCacheLog = new TakCacheLog();
    takCacheLog.setRefreshSuccessful(false);
    takCacheLog.setRefreshStatus(RefreshStatus.REFRESH_FAILED);
    return takCacheLog;
  }

}
