package se.skl.tp.vp.wsdl.utils;

import java.net.URL;
import org.dom4j.Node;

public class WsdlAddressNodeHandler extends WsdlNodeHandler{

  public WsdlAddressNodeHandler(URL url) {
    super(url);
  }

  @Override
  String getQuery(Node node) {
    return null;
  }
}
