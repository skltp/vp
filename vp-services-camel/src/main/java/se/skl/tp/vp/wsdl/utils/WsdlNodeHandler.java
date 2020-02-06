package se.skl.tp.vp.wsdl.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.dom4j.Node;
import se.skl.tp.vp.xmlutil.NodeHandler;

public abstract class WsdlNodeHandler implements NodeHandler {
  private URL url;

  public WsdlNodeHandler(URL url) {
    this.url = url;
  }


  protected String replaceBaseUrlParts(URL url, String pQuery) {
    try {
      URI uriNew =
          new URI(
              url.getProtocol(),
              null,
              url.getHost(),
              url.getPort(),
              url.getPath(),
              pQuery,
              null);
      return uriNew.toURL().toExternalForm();
    } catch (URISyntaxException | MalformedURLException e) {
      throw new WsdlNodeHandlerException("Error transforming url", e);
    }

  }

  @Override
  public void handle(Node node) {
    node.setText(replaceBaseUrlParts(url, getQuery(node)));
  }

  abstract String getQuery(Node node);

  public class WsdlNodeHandlerException extends RuntimeException {
    WsdlNodeHandlerException(String msg, Exception e) {
      super(msg, e);
    }
  }
}
