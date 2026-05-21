package se.skl.tp.vp.camel;

import java.io.File;
import java.io.FileOutputStream;
import org.apache.camel.converter.IOConverter;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.skl.tp.vp.util.LeakDetectionBaseTest;

class BaseNettyTest extends CamelTestSupport {
  protected static final Logger LOG = LoggerFactory.getLogger(BaseNettyTest.class);

  private static volatile int port;

  @BeforeAll
  static void initPort() throws Exception {
    File file = new File("target/nettyport.txt");

    if (!file.exists()) {
      // start from somewhere in the 26xxx range
      port = AvailablePortFinder.getNextAvailable(26000, 27000);
    } else {
      // read port number from file
      String s = IOConverter.toString(file, null);
      port = Integer.parseInt(s);
      // use next free port
      port = AvailablePortFinder.getNextAvailable(port + 1, port + 1000);
    }
    LeakDetectionBaseTest.startLeakDetection();
  }

  @AfterAll
  static void savePort() throws Exception {
    File file = new File("target/nettyport.txt");

    // save to file, do not append
    try (FileOutputStream fos = new FileOutputStream(file, false)) {
        fos.write(String.valueOf(port).getBytes());
    }
    LeakDetectionBaseTest.verifyNoLeaks();
  }

  @BeforeEach
  void init() throws Exception {
      PropertiesComponent pc = context.getPropertiesComponent();
		pc.setLocation("ref:prop");
	    context.getRegistry().bind("properties", pc);	
  }


  protected int getNextPort() {
    port = AvailablePortFinder.getNextAvailable(port + 1, port + 1000);
    return port;
  }

  protected int getPort() {
    return port;
  }

}
