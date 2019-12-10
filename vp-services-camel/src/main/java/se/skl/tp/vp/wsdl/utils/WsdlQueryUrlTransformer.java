package se.skl.tp.vp.wsdl.utils;

import static se.skl.tp.vp.wsdl.utils.WsdlBomHandler.removeCharsBeforeXmlDeclaration;
import static se.skl.tp.vp.xmlutil.XmlHelper.applyHandlingToNodes;
import static se.skl.tp.vp.xmlutil.XmlHelper.createXPath;

import java.net.URL;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;

public class WsdlQueryUrlTransformer {

  private static XPath getAlladressLocation =
      createXPath(
          "/wsdl:definitions/wsdl:service/wsdl:port/soap:address/@location",
          "soap=http://schemas.xmlsoap.org/wsdl/soap/",
          "wsdl=http://schemas.xmlsoap.org/wsdl/",
          "xsd=http://www.w3.org/2001/XMLSchema");

  private static XPath getAllXsdImports =
      createXPath(
          "//xsd:import/@schemaLocation",
          "soap=http://schemas.xmlsoap.org/wsdl/soap/",
          "wsdl=http://schemas.xmlsoap.org/wsdl/",
          "xsd=http://www.w3.org/2001/XMLSchema");

  // Static utility class
  private WsdlQueryUrlTransformer() {}

  public static String replaceUrlParts(String wsdlOrXsdSource, URL url) throws DocumentException {
    Document wsdl = DocumentHelper.parseText(removeCharsBeforeXmlDeclaration(wsdlOrXsdSource));

    replaceUrlPartsToService(url, wsdl);
    replaceUrlPartsToXsd(url, wsdl);
    return wsdl.asXML();
  }

  private static void replaceUrlPartsToXsd(URL url, Document wsdlOrXsd) {
    applyHandlingToNodes(wsdlOrXsd, getAlladressLocation, new WsdlAddressNodeHandler(url));
  }

  private static void replaceUrlPartsToService(URL url, Document wsdlOrXsd) {
    applyHandlingToNodes(wsdlOrXsd, getAllXsdImports, new WsdlSchemaImportNodeHandler(url));
  }
}
