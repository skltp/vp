package se.skl.tp.vp.xmlutil;

import static se.skl.tp.vp.wsdl.PathHelper.getPath;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import org.dom4j.xpath.DefaultXPath;
import org.jaxen.SimpleNamespaceContext;

public class XmlHelper {

  private XmlHelper(){

  }

  //Note only supposed to be used by any other methods than #openDocument otherwise might
  // not be thread safe
  private static final SAXReader reader;

  static {
    reader = new SAXReader();
  }

  public static XPath createXPath(String pXpath, String... namespaces) {
    XPath xpath = new DefaultXPath(pXpath);
    setNameSpaces(namespaces, xpath);
    return xpath;
  }

  /**
   * Note that invalid xPaths, null arguments, source without matching element etc will result in
   * all kind of havoc.
   *
   * @param namespaces zero or more namespaces on the form ns0=http://my.magic.org
   */
  public static String selectXPathStringValue(
      Document pSourceDocument, String pXpath, String... namespaces) {
    return createXPath(pXpath, namespaces).selectSingleNode(pSourceDocument).getStringValue();
  }

  public static void applyHandlingToNodes(
      Document pSourceDocument, XPath xpath, NodeHandler handler) {
    List nodes = xpath.selectNodes(pSourceDocument);
    for (Object node :nodes)
      handler.handle((Node) node);
  }

  private static void setNameSpaces(String[] namespaces, XPath xpathDestination) {
    if (namespaces == null || namespaces.length == 0) {
      return;
    }
    HashMap<String, String> nameSpaceMap = new HashMap<>();
    for (String tuple : namespaces) {
      String[] tupleArr = tuple.split("=");

      nameSpaceMap.put(tupleArr[0], tupleArr[1]);
    }
    xpathDestination.setNamespaceContext(new SimpleNamespaceContext(nameSpaceMap));
  }

  /**
   * @param documentPath prefix "classpath:" may be used and expanded to source folder
   */
  public static Document openDocument(String documentPath) throws DocumentException, FileNotFoundException, URISyntaxException {
   return reader.read(getPath(documentPath).toFile());
  }
}
