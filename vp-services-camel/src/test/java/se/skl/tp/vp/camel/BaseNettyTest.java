package se.skl.tp.vp.camel;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import org.apache.camel.CamelContext;
import org.apache.camel.converter.IOConverter;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.skl.tp.vp.util.LeakDetectionBaseTest;


public class BaseNettyTest extends CamelTestSupport {
  protected static final Logger LOG = LoggerFactory.getLogger(BaseNettyTest.class);

  private static volatile int port;

  @BeforeAll
  public static void initPort() throws Exception {
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
  public static void savePort() throws Exception {
    File file = new File("target/nettyport.txt");

    // save to file, do not append
    FileOutputStream fos = new FileOutputStream(file, false);
    try {
      fos.write(String.valueOf(port).getBytes());
    } finally {
      fos.close();
    }
    LeakDetectionBaseTest.verifyNoLeaks();
  }

  @Override
  protected CamelContext createCamelContext() throws Exception {
    CamelContext context = super.createCamelContext();
	PropertiesComponent pc = context.getPropertiesComponent();
	pc.setLocation("ref:prop");
    return context;
  }
  
  @Override
  protected Registry createCamelRegistry() throws Exception {
    Registry registry = super.createCamelRegistry();

    Properties prop = new Properties();
    prop.setProperty("port", "" + getPort());
    registry.bind("prop", prop);
    return registry;
  }


  protected int getNextPort() {
    port = AvailablePortFinder.getNextAvailable(port + 1, port + 1000);
    return port;
  }

  protected int getPort() {
    return port;
  }

}
