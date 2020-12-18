package se.skl.tp.vp.utils;

import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import sun.misc.SharedSecrets;
import sun.misc.VM;

public class MemoryUtil {

  private MemoryUtil() {}


  private static MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();

  public static String getMemoryUsed() {
    return bytesReadable(SharedSecrets.getJavaNioAccess().getDirectBufferPool().getMemoryUsed());
  }

  public static String getTotalCapacity() {
    return bytesReadable(SharedSecrets.getJavaNioAccess().getDirectBufferPool().getTotalCapacity());
  }

  public static String getVMMaxMemory() {
    return bytesReadable(VM.maxDirectMemory());
  }

  public static long getCount() {
    return SharedSecrets.getJavaNioAccess().getDirectBufferPool().getCount();
  }

  public static MemoryUsage getNonHeapMemoryUsage() {
    return mbean.getNonHeapMemoryUsage();
  }

  public static PooledByteBufAllocatorMetric getNettyPooledByteBufMetrics() {
    return PooledByteBufAllocator.DEFAULT.metric();
  }


  public static Map getNettyMemoryMap(){
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();

    PooledByteBufAllocatorMetric nettyMetrics = MemoryUtil.getNettyPooledByteBufMetrics();

    long totActiveAllocations=0;
    int arenaNum =0;

    for (PoolArenaMetric poolArenaMetric : nettyMetrics.directArenas()) {
      long activeAllocations =  poolArenaMetric.numActiveAllocations();
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
          String.format("direct bytes:%s(active allocs:%d, Arenas:%d), Heap bytes: %s(Arenas:%d), ThreadCaches: %d",
          nettyMetrics.usedDirectMemory(),
          totActiveAllocations,
          nettyMetrics.numDirectArenas(),
          nettyMetrics.usedHeapMemory(),
          nettyMetrics.numHeapArenas(),
          nettyMetrics.numThreadLocalCaches()));

    return map;
  }

  public static String getNettyMemoryJsonString() {
    final Map nettyMemoryMap = getNettyMemoryMap();
    try {
      return  new JSONObject(nettyMemoryMap).toString(2).replace("\\/", "/");
    } catch (JSONException e) {
      return  new JSONObject(nettyMemoryMap).toString();
    }
  }

  public static String bytesReadable(long v) {
    if (v < 1024) {
      return v + " B";
    }
    int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
    return String.format("%.1f %sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
  }
}
