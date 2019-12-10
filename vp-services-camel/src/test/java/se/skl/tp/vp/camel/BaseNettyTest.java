package se.skl.tp.vp.camel;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.converter.IOConverter;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.skl.tp.vp.util.LeakDetectionBaseTest;


public class BaseNettyTest extends CamelTestSupport {
  protected static final Logger LOG = LoggerFactory.getLogger(BaseNettyTest.class);

  private static volatile int port;

  @BeforeClass
  public static void initPort() throws Exception {
    File file = new File("target/nettyport.txt");

    if (!file.exists()) {
      // start from somewhere in the 26xxx range
      port = AvailablePortFinder.getNextAvailable(26000);
    } else {
      // read port number from file
      String s = IOConverter.toString(file, null);
      port = Integer.parseInt(s);
      // use next free port
      port = AvailablePortFinder.getNextAvailable(port + 1);
    }
    LeakDetectionBaseTest.startLeakDetection();
  }

  @AfterClass
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
    context.addComponent("properties", new PropertiesComponent("ref:prop"));
    return context;
  }

  @Override
  protected JndiRegistry createRegistry() throws Exception {
    JndiRegistry jndi = super.createRegistry();

    Properties prop = new Properties();
    prop.setProperty("port", "" + getPort());
    jndi.bind("prop", prop);

    return jndi;
  }

  protected int getNextPort() {
    port = AvailablePortFinder.getNextAvailable(port + 1);
    return port;
  }

  protected int getPort() {
    return port;
  }

}
