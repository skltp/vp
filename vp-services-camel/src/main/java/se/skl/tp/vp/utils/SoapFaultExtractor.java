package se.skl.tp.vp.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for extracting SOAP fault information from XML message bodies.
 * Supports SOAP 1.1 fault format.
 */
@SuppressWarnings("HttpUrlsUsage")
public class SoapFaultExtractor {

  public static final String ELEMENT_FAULT = "Fault";
  public static final String ELEMENT_FAULTCODE = "faultcode";
  public static final String ELEMENT_FAULTSTRING = "faultstring";
  public static final String ELEMENT_DETAIL = "detail";
  public static final String ANY_NAMESPACE = "*";

  private DocumentBuilderFactory documentBuilderFactory;
  private TransformerFactory transformerFactory;

  /**
   * Extracts SOAP 1.1 fault information from an XML message body.
   *
   * @param messageBody The XML message body as a string
   * @return SoapFaultInfo containing extracted fault details, or an empty record if no fault found
   */
  public SoapFaultInfo extractSoapFault(String messageBody) {
    if (messageBody == null) {
      return new SoapFaultInfo(null, null, null);
    }

    try {
      DocumentBuilder builder = getDocumentBuilderFactory().newDocumentBuilder();
      Document doc = builder.parse(new ByteArrayInputStream(messageBody.getBytes(StandardCharsets.UTF_8)));

      NodeList faultNodes = doc.getElementsByTagNameNS(ANY_NAMESPACE, ELEMENT_FAULT);
      if (faultNodes.getLength() > 0) {
        Element fault = (Element) faultNodes.item(0);

        String faultCode = getElementText(fault, ELEMENT_FAULTCODE);
        String faultString = getElementText(fault, ELEMENT_FAULTSTRING);
        String detail = getDetailAsXml(fault);

        return new SoapFaultInfo(faultCode, faultString, detail);
      }
    } catch (Exception e) {
      // Return partial fault info with error message
      return new SoapFaultInfo(null, "Unknown SOAPFault (%s)".formatted(e.getMessage()), null);
    }

    return new SoapFaultInfo(null, null, null);
  }

  /**
   * Extracts text content from a child element.
   */
  private String getElementText(Element parent, String tagName) {
    NodeList nodes = parent.getElementsByTagNameNS(ANY_NAMESPACE, tagName);
    if (nodes.getLength() > 0) {
      return nodes.item(0).getTextContent();
    }
    return null;
  }

  /**
   * Serializes the detail element to XML string, preserving structure.
   * This is used for the detail element which may contain application-specific structured data.
   */
  private String getDetailAsXml(Element parent) {
    NodeList nodes = parent.getElementsByTagNameNS(ANY_NAMESPACE, ELEMENT_DETAIL);
    if (nodes.getLength() > 0) {
      Element element = (Element) nodes.item(0);
      return serializeElement(element);
    }
    return null;
  }

  /**
   * Serializes an XML element to a string, preserving its full structure.
   */
  private String serializeElement(Element element) {
    try {
      Transformer transformer = getTransformerFactory().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(element), new StreamResult(writer));
      return writer.toString();
    } catch (Exception e) {
      // Fallback to text content if serialization fails
      return element.getTextContent();
    }
  }

  DocumentBuilderFactory getDocumentBuilderFactory() throws ParserConfigurationException {
    if (documentBuilderFactory != null) {
      return documentBuilderFactory;
    }
    documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    documentBuilderFactory.setXIncludeAware(false);
    documentBuilderFactory.setExpandEntityReferences(false);
    return documentBuilderFactory;
  }

  TransformerFactory getTransformerFactory() throws TransformerConfigurationException {
    if (transformerFactory != null) {
      return transformerFactory;
    }
    transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
    transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
    return transformerFactory;
  }
}
