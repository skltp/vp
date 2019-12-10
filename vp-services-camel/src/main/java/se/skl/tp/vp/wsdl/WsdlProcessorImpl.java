package se.skl.tp.vp.wsdl;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.commons.lang.CharEncoding.UTF_8;
import static se.skl.tp.vp.wsdl.PathHelper.getPath;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.Map;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.schema.SchemaReference;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.wsdl.utils.ForwardedProxyUtil;
import se.skl.tp.vp.wsdl.utils.ForwardedProxyUtil.ProxyUrl;
import se.skl.tp.vp.wsdl.utils.WsdlDefinitionHandler;
import se.skl.tp.vp.wsdl.utils.WsdlQueryUrlTransformer;

@Service
public class WsdlProcessorImpl implements WsdlProcessor {


  @Autowired
  private ForwardedProxyUtil forwardedProxyUtil;

  @Autowired
  private WsdlConfiguration wsdlConfiguration;

  WsdlDefinitionHandler wsdlDefinitionHandler;

  public WsdlProcessorImpl() {
    wsdlDefinitionHandler = new WsdlDefinitionHandler();
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    URL url = createUrl(exchange);
    WsdlConfig wsdlConfig = wsdlConfiguration.getOnWsdlUrl(url.getPath());
    if (wsdlConfig == null) {
      exchange.getIn().setBody(createErrorMsgWithValidPaths(url));
      exchange.getIn().setHeader(CONTENT_TYPE, "text/plain;UTF-8");
      exchange.getIn().setHeader(HTTP_RESPONSE_CODE, 500);
      exchange.setProperty(Exchange.CHARSET_NAME, UTF_8);
      return;
    }

    String wsdlFileName = wsdlConfig.getWsdlfilepath();
    String fileName = isXsdCall(exchange) ? getXsdFileName(exchange, wsdlFileName) : wsdlFileName;
    String wsdlOrXsd = getWsdlOrXsd(fileName);
    exchange.getIn().setBody(WsdlQueryUrlTransformer.replaceUrlParts(wsdlOrXsd, url));
    exchange.getIn().setHeader(CONTENT_TYPE, "text/xml;UTF-8");
    exchange.setProperty(Exchange.CHARSET_NAME, UTF_8);
  }

  private String getXsdFileName(Exchange exchange, String wsdlFile) throws WSDLException {
    try {
      String xsdKey = URLDecoder.decode(exchange.getIn().getHeader("xsd", String.class), "utf-8");
      Map<String, SchemaReference> schemas = wsdlDefinitionHandler.getAllSchemaReferences(wsdlFile);
      SchemaReference schemaReference = schemas.get(xsdKey);
      if (schemaReference == null) {
        throw new WSDLException("OTHER_ERROR", "Couldn't find xsd named " + xsdKey);
      }

      return schemaReference.getReferencedSchema().getDocumentBaseURI();

    } catch (UnsupportedEncodingException e) {
      throw new WSDLException("OTHER_ERROR", e.getMessage());
    }
  }

  private boolean isXsdCall(Exchange exchange) {
    return exchange.getIn().getHeaders().containsKey("xsd");
  }

  private URL createUrl(Exchange exchange) throws MalformedURLException {
    URL localUrl = exchange.getIn().getHeader(Exchange.HTTP_URL, URL.class);
    ProxyUrl proxyUrl = forwardedProxyUtil.getForwardProxyUrl(exchange);

    String prot = proxyUrl.getProtocol() != null ? proxyUrl.getProtocol() : localUrl.getProtocol();
    String host = proxyUrl.getHost() != null ? proxyUrl.getHost() : localUrl.getHost();
    int port = proxyUrl.getPort() != null ? Integer.valueOf(proxyUrl.getPort()) : localUrl.getPort();
    String file = localUrl.getFile();

    return new URL(prot, host, port, file);
  }


  private String createErrorMsgWithValidPaths(URL url) {
    StringBuilder builder = new StringBuilder();
    builder.append("No wsdl found on this path, following wsdl paths is available:");
    for (String wsdlUrl : wsdlConfiguration.getAllWsdlUrl()) {
      builder.append(System.lineSeparator());
      builder.append(String.format("%s://%s/%s?wsdl", url.getProtocol(), url.getAuthority(), wsdlUrl));
    }
    return builder.toString();
  }

  private String getWsdlOrXsd(String wsdlOrXsdFileName) throws IOException, URISyntaxException {
    return new String(Files.readAllBytes(getPath(wsdlOrXsdFileName)));
  }

}
