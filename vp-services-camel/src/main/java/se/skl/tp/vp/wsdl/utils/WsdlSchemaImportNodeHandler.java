package se.skl.tp.vp.wsdl.utils;

import java.net.URL;
import org.dom4j.Node;

public class WsdlSchemaImportNodeHandler extends WsdlNodeHandler{

  public WsdlSchemaImportNodeHandler(URL url) {
    super(url);
  }

  @Override
  String getQuery(Node node) {
    return "xsd="+node.getText();
  }
}
