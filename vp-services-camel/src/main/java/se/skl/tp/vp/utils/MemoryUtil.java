package se.skl.tp.vp.utils;

import com.sun.management.HotSpotDiagnosticMXBean;
import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;

import java.lang.management.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class MemoryUtil {

  private MemoryUtil() {
  }

  private static final BufferPoolMXBean directBufferPool = getDirectBufferPool();
  private static final HotSpotDiagnosticMXBean hotSpotDiagnostic = getHotSpotDiagnostic();
  private static final MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();

  public static String getMemoryUsed() {
    return bytesReadable(directBufferPool.getMemoryUsed());
  }

  public static String getTotalCapacity() {
    return bytesReadable(directBufferPool.getTotalCapacity());
  }

  public static String getVMMaxMemory() {
    try {
      String rawValue = hotSpotDiagnostic.getVMOption("MaxDirectMemorySize").getValue();
      return bytesReadable(Long.parseLong(rawValue));
    }
    catch (Exception e) {
      return "";
    }
  }

  public static long getCount() {
    return directBufferPool.getCount();
  }

  public static MemoryUsage getNonHeapMemoryUsage() {
    return mbean.getNonHeapMemoryUsage();
  }

  public static PooledByteBufAllocatorMetric getNettyPooledByteBufMetrics() {
    return PooledByteBufAllocator.DEFAULT.metric();
  }


  public static Map<String, Object> getNettyMemoryMap() {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();

    PooledByteBufAllocatorMetric nettyMetrics = MemoryUtil.getNettyPooledByteBufMetrics();

    long totActiveAllocations = 0;
    int arenaNum = 0;

    for (PoolArenaMetric poolArenaMetric : nettyMetrics.directArenas()) {
      long activeAllocations = poolArenaMetric.numActiveAllocations();
      totActiveAllocations += activeAllocations;
      map.put("DirectArena" + (++arenaNum),
          String.format("active alloc:%d(alloc:%d, dealloc:%d), ActiveBytes: %d, ThreadCaches: %d",
              activeAllocations,
              poolArenaMetric.numAllocations(),
              poolArenaMetric.numDeallocations(),
              poolArenaMetric.numActiveBytes(),
              poolArenaMetric.numThreadCaches()));
    }
    map.put("NettyTotal",
        String.format(
            "direct bytes:%s(active allocs:%d, Arenas:%d), Heap bytes: %s(Arenas:%d), ThreadCaches: %d",
            nettyMetrics.usedDirectMemory(),
            totActiveAllocations,
            nettyMetrics.numDirectArenas(),
            nettyMetrics.usedHeapMemory(),
            nettyMetrics.numHeapArenas(),
            nettyMetrics.numThreadLocalCaches()));

    return map;
  }

  public static String getNettyMemoryJsonString() {
    final Map<String, Object> nettyMemoryMap = getNettyMemoryMap();
    try {
      return new JSONObject(nettyMemoryMap).toString(2).replace("\\/", "/");
    } catch (JSONException e) {
      return new JSONObject(nettyMemoryMap).toString();
    }
  }

  public static String bytesReadable(long v) {
    if (v < 1024) {
      return v + " B";
    }
    int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
    return String.format("%.1f %sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
  }

  private static BufferPoolMXBean getDirectBufferPool() {
    for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
      if (pool.getName().equals("direct")) {
        return pool;
      }
    }
    throw new RuntimeException("Could not find direct BufferPoolMXBean");
  }

  private static HotSpotDiagnosticMXBean getHotSpotDiagnostic() {
    HotSpotDiagnosticMXBean hsdiag = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
    if (hsdiag == null) {
      throw new RuntimeException("Could not find HotSpotDiagnosticMXBean");
    }
    return hsdiag;
  }
}
