package se.skl.tp.vp.util;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ResourceLeakDetector;
import java.util.Collection;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.AfterClass;
import org.junit.BeforeClass;


@Log4j2
public class LeakDetectionBaseTest {

  @BeforeClass
  public static void startLeakDetection() {
    if( isLeakDectectionAcivated()) {
      System.setProperty("io.netty.leakDetection.maxRecords", "100");
      System.setProperty("io.netty.leakDetection.targetRecords", "10");
      System.setProperty("io.netty.leakDetection.acquireAndReleaseOnly", "true");
      ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    }
  }

  @AfterClass
  public static void verifyNoLeaks() throws Exception {
    if( isLeakDectectionAcivated()) {
      //Force GC to bring up leaks
      System.gc();
      //Kick leak detection logging
      ByteBufAllocator.DEFAULT.buffer(1).release();
      Collection<LogEvent> events = TestLogAppender.getLeakEvents();
      if (!events.isEmpty()) {
        String message = "Leaks detected while running tests: " + events;
        TestLogAppender.clearLeakEvents();
        throw new AssertionError(message);
      }
    }
  }

  private static boolean isLeakDectectionAcivated(){
    String profiles = System.getProperty("spring.profiles.active");
    return profiles != null && profiles.contains("leak");
  }

}
