package se.skl.tp.vp.vagvalagent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.skl.tp.DefaultRoutingConfiguration;
import se.skl.tp.behorighet.BehorighetHandler;
import se.skl.tp.behorighet.BehorighetHandlerImpl;
import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.vagval.VagvalHandler;
import se.skl.tp.vagval.VagvalHandlerImpl;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.MessageProperties;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skltp.takcache.RoutingInfo;
import se.skltp.takcache.TakCache;
import se.skltp.takcache.TakCacheLog;
import se.skltp.takcache.TakCacheLog.RefreshStatus;

public class VagvalAgent implements VagvalAgentInterface {

  private static final Logger LOGGER = LoggerFactory.getLogger(VagvalAgent.class);

  private MessageProperties messageProperties;
  private BehorighetHandlerImpl behorighetHandler;
  private VagvalHandler vagvalHandler;
  private TakCache takCache;
  private TakCacheLog takCacheLog = null;
  DefaultRoutingConfiguration defaultRoutingConfiguration;

  public VagvalAgent(){
  }

  public VagvalAgent(HsaCache hsaCache, TakCache takCache, DefaultRoutingConfiguration defaultRoutingConfiguration) {
    init(hsaCache,takCache, defaultRoutingConfiguration);
  }

  protected void init(HsaCache hsaCache, TakCache takCache, DefaultRoutingConfiguration defaultRoutingConfiguration){
    behorighetHandler = new BehorighetHandlerImpl(hsaCache, takCache, defaultRoutingConfiguration);
    vagvalHandler = new VagvalHandlerImpl(hsaCache, takCache, defaultRoutingConfiguration);
    this.takCache = takCache;
  }

  @Override
  public TakCacheLog resetVagvalCache(){
    takCacheLog = takCache.refresh();
    return takCacheLog;
  }

  @Override
  public List<RoutingInfo> visaVagval(VisaVagvalRequest request) {
      if(takCacheLog == null){
        resetVagvalCache();
      }

    if (!isTakCacheInitialized()) {
      String errorMessage = messageProperties.get(VpSemanticErrorCodeEnum.VP008, null);
      LOGGER.error(errorMessage);
      throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP008);
    }

    List<RoutingInfo> routingInfo = vagvalHandler.getRoutingInfo(request.getTjanstegranssnitt(), request.getReceiverId());
    if(routingInfo == null || routingInfo.isEmpty()){
      return routingInfo;
    }

    if (!behorighetHandler.isAuthorized(request.getSenderId(), request.getTjanstegranssnitt(), request.getReceiverId())) {
      throwNotAuthorizedException(request);
    }

    return routingInfo;
  }

  @Override
  public int getNumberOfVagval() {
    return takCacheLog==null ? 0: takCacheLog.getNumberVagval();
  }

  @Override
  public int getNumberOfBehorigheter() {
    return takCacheLog==null ? 0: takCacheLog.getNumberBehorigheter();
  }

  @Deprecated
  public List<AnropsBehorighetsInfoType> getAnropsBehorighetsInfoList() {
      if(takCacheLog==null){
        resetVagvalCache();
      }
      return (takCache == null) ? Collections.emptyList() : takCache.getAnropsBehorighetsInfos();
  }

  private boolean isTakCacheInitialized(){
    return takCacheLog!=null && takCacheLog.getRefreshStatus()!=RefreshStatus.REFRESH_FAILED;
  }

  private void throwNotAuthorizedException(VisaVagvalRequest request) {
    String errorMessage = messageProperties.get(VpSemanticErrorCodeEnum.VP007,getRequestSummary(request));
    LOGGER.info(errorMessage);
    throw new VpSemanticException(errorMessage, VpSemanticErrorCodeEnum.VP007);
  }

  private String getRequestSummary(VisaVagvalRequest request) {
    return
        "serviceNamespace: " + request.getTjanstegranssnitt()
            + ", receiverId: " + request.getReceiverId()
            + ", senderId: " + request.getSenderId();
  }

  public void setMessageProperties(MessageProperties messageProperties) {
    this.messageProperties = messageProperties;
  }

}
