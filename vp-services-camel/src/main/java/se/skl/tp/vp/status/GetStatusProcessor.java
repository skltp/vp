package se.skl.tp.vp.status;

import io.netty.buffer.PooledByteBufAllocatorMetric;
import java.lang.management.MemoryUsage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.impl.EventDrivenConsumerRoute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.service.HsaCacheService;
import se.skl.tp.vp.service.HsaCacheStatus;
import se.skl.tp.vp.service.TakCacheService;
import se.skl.tp.vp.utils.MemoryUtil;
import se.skltp.takcache.TakCacheLog;

@Service
public class GetStatusProcessor implements Processor {

  public static final String KEY_APP_NAME = "Name";
  public static final String KEY_APP_VERSION = "Version";
  public static final String KEY_APP_BUILD_TIME = "BuildTime";
  public static final String KEY_SERVICE_STATUS = "ServiceStatus";
  public static final String KEY_UPTIME = "Uptime";
  public static final String KEY_MANAGEMENT_NAME = "ManagementName";
  public static final String KEY_JAVA_VERSION = "JavaVersion";
  public static final String KEY_CAMEL_VERSION = "CamelVersion";
  public static final String KEY_TAK_CACHE_INITIALIZED = "TakCacheInitialized";
  public static final String KEY_TAK_CACHE_RESET_INFO = "TakCacheResetInfo";
  public static final String KEY_HSA_CACHE_INITIALIZED = "HsaCacheInitialized";
  public static final String KEY_HSA_CACHE_RESET_INFO = "HsaCacheResetInfo";
  public static final String KEY_JVM_TOTAL_MEMORY = "JvmTotalMemory";
  public static final String KEY_JVM_FREE_MEMORY = "JvmFreeMemory";
  public static final String KEY_JVM_USED_MEMORY = "JvmUsedMemory";
  public static final String KEY_JVM_MAX_MEMORY = "JvmMaxMemory";
  public static final String KEY_DIRECT_MEMORY = "DirectMemBufferPool";
  public static final String KEY_NON_HEAP_MEMORY = "NonHeapMemory";
  public static final String KEY_VM_MAX_DIRECT_MEMORY = "MaxDirectMemory";
  public static final String KEY_NETTY_DIRECT_MEMORY = "NettyDirectMemory";
  public static final String KEY_ENDPOINTS = "Endpoints";
  @Autowired
  private CamelContext camelContext;

  @Autowired
  TakCacheService takService;

  @Autowired
  HsaCacheService hsaService;

  @Autowired
  BuildProperties buildProperties;

  @Override
  public void process(Exchange exchange) {
    boolean showNettyMemory = exchange.getIn().getHeaders().containsKey("netty");
    boolean showExtendedMemory = exchange.getIn().getHeaders().containsKey("memory");
    if (showNettyMemory) {
      exchange.getIn().setBody(MemoryUtil.getNettyMemoryJsonString());
    } else {
      JSONObject jsonObject = new JSONObject(registerInfo(showExtendedMemory));
      try {
          exchange.getIn().setBody(jsonObject.toString(2).replace("\\/", "/"));
      } catch (JSONException e) {
        exchange.getIn().setBody(jsonObject.toString());
      }
    }
    exchange.getIn().getHeaders().put(HttpHeaders.HEADER_CONTENT_TYPE, "application/json");
  }

  private Map<String, Object> registerInfo(boolean showMemory) {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();

    map.put(KEY_APP_NAME, buildProperties.getName());
    map.put(KEY_APP_VERSION, buildProperties.getVersion());
    map.put(KEY_APP_BUILD_TIME, buildProperties.getTime());

    ServiceStatus serviceStatus = camelContext.getStatus();
    map.put(KEY_SERVICE_STATUS, "" + serviceStatus);
    map.put(KEY_UPTIME, camelContext.getUptime());
    map.put(KEY_MANAGEMENT_NAME, camelContext.getManagementName());
    map.put(KEY_JAVA_VERSION, (String) System.getProperties().get("java.version"));
    map.put(KEY_CAMEL_VERSION, camelContext.getVersion());

    map.put(KEY_TAK_CACHE_INITIALIZED, "" + takService.isInitalized());
    map.put(KEY_TAK_CACHE_RESET_INFO, getTakRefreshInfo());

    HsaCacheStatus hsaCacheStatus = hsaService.getHsaCacheStatus();
    map.put(KEY_HSA_CACHE_INITIALIZED, "" + hsaCacheStatus.isInitialized());
    map.put(KEY_HSA_CACHE_RESET_INFO, getHsaRefreshInfo(hsaCacheStatus));

    Runtime instance = Runtime.getRuntime();
    map.put(KEY_JVM_TOTAL_MEMORY, "" + MemoryUtil.bytesReadable(instance.totalMemory()));
    map.put(KEY_JVM_FREE_MEMORY, "" + MemoryUtil.bytesReadable(instance.freeMemory()));
    map.put(KEY_JVM_USED_MEMORY, "" + MemoryUtil.bytesReadable((instance.totalMemory() - instance.freeMemory())));
    map.put(KEY_JVM_MAX_MEMORY, "" + MemoryUtil.bytesReadable(instance.maxMemory()));
    if (showMemory) {
      map.put(KEY_DIRECT_MEMORY, "" + GetDirectMemoryString());
      map.put(KEY_VM_MAX_DIRECT_MEMORY, "" + MemoryUtil.getVMMaxMemory());
      map.put(KEY_NON_HEAP_MEMORY, "" + getNonHeapMemory());
      map.put(KEY_NETTY_DIRECT_MEMORY, "" + getNettyDirectMemory());
    }
    map.put(KEY_ENDPOINTS, getEndpointInfo());
    return map;
  }

  private String getNonHeapMemory() {
    MemoryUsage nonHeapMemoryUsage = MemoryUtil.getNonHeapMemoryUsage();

    return String.format("Init: %s Used: %s, Commited: %s, Max: %s",
        MemoryUtil.bytesReadable(nonHeapMemoryUsage.getInit()),
        MemoryUtil.bytesReadable(nonHeapMemoryUsage.getUsed()),
        MemoryUtil.bytesReadable(nonHeapMemoryUsage.getCommitted()),
        MemoryUtil.bytesReadable(nonHeapMemoryUsage.getMax()));
  }

  private String getNettyDirectMemory() {
    PooledByteBufAllocatorMetric nettyMetrics = MemoryUtil.getNettyPooledByteBufMetrics();
    String usedDirectMem = MemoryUtil.bytesReadable(nettyMetrics.usedDirectMemory());
    String usedHeapMem = MemoryUtil.bytesReadable(nettyMetrics.usedHeapMemory());

    return String.format("Direct: %s(Arenas:%d), Heap: %s(Arenas:%d), ThreadCaches: %d",
        usedDirectMem,
        nettyMetrics.numDirectArenas(),
        usedHeapMem,
        nettyMetrics.numHeapArenas(),
        nettyMetrics.numThreadLocalCaches());
  }

  private String GetDirectMemoryString() {
    return String.format("Used: %s, Count: %d, Max Capacity: %s",
        MemoryUtil.getMemoryUsed(),
        MemoryUtil.getCount(),
        MemoryUtil.getTotalCapacity());
  }

  private List getEndpointInfo() {
    List<String> endPoints = new ArrayList<>();
    List<Route> routes = camelContext.getRoutes();
    for (Route route : routes) {
      String endpoint = route.getEndpoint().getEndpointKey();
      if (endpoint.startsWith("http") && ((EventDrivenConsumerRoute) route).getStatus() == ServiceStatus.Started) {
        endPoints.add(route.getEndpoint().getEndpointKey());
      }
    }
    return endPoints;
  }

  public String getHsaRefreshInfo(HsaCacheStatus hsaCacheStatus) {
    return String.format("Date:%s Status:%s oldNum:%d newNum:%d",
        getFormattedDate(hsaCacheStatus.getResetDate()),
        hsaCacheStatus.isInitialized(),
        hsaCacheStatus.getNumInCacheOld(),
        hsaCacheStatus.getNumInCacheNew());
  }

  public String getTakRefreshInfo() {
    TakCacheLog takCacheLog = takService.getLastRefreshLog();
    if (takCacheLog == null) {
      return "Not initialized";
    }

    return String.format("Date:%s Status:%s vagval:%d behorigheter:%d",
        getFormattedDate(takService.getLastResetDate()),
        takCacheLog.getRefreshStatus(),
        takCacheLog.getNumberVagval(),
        takCacheLog.getNumberBehorigheter());
  }

  private String getFormattedDate(Date date) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    return date == null ? "" : dateFormat.format(date);
  }

}
