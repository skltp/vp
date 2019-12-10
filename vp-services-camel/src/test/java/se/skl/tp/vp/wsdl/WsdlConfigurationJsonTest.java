package se.skl.tp.vp.wsdl;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import se.skl.tp.vp.config.ProxyHttpForwardedHeaderProperties;
import se.skl.tp.vp.constants.PropertyConstants;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ProxyHttpForwardedHeaderProperties.class)
public class WsdlConfigurationJsonTest {

  @Value("${" + PropertyConstants.WSDL_JSON_FILE + "}")
  private String wsdlConfigJSonFile;
  @Value("${" + PropertyConstants.WSDLFILES_DIRECTORY + "}")
  private String wsdlDir;


  @Test
  public void testWsdlConfiguration() throws IOException, URISyntaxException {

    WsdlConfiguration config = new WsdlConfigurationJson(wsdlConfigJSonFile,wsdlDir);
    List<String> configs = config.getAllWsdlUrl();
    configs.forEach(wsdUrls->System.out.println(wsdUrls));
    assertTrue(configs.size()>0);
  }

}