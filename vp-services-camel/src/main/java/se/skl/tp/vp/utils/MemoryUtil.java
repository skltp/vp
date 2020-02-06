package se.skl.tp.vp.utils;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import sun.misc.SharedSecrets;
import sun.misc.VM;

public class MemoryUtil {
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


  public static String bytesReadable(long v) {
    if (v < 1024) {
      return v + " B";
    }
    int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
    return String.format("%.1f %sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
  }
}
