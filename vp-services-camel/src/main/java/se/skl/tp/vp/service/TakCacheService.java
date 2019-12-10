package se.skl.tp.vp.service;

import java.util.Date;
import java.util.List;
import se.skltp.takcache.RoutingInfo;
import se.skltp.takcache.TakCacheLog;

public interface TakCacheService {

  TakCacheLog refresh();

  boolean isInitalized();

  boolean isAuthorized(String senderId, String servicecontractNamespace, String receiverId);

  List<RoutingInfo> getRoutingInfo(String tjanstegranssnitt, String receiverAddress);

  Date getLastResetDate();

  TakCacheLog getLastRefreshLog();
}
