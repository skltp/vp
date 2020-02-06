package se.skl.tp.vp.wsdl;

import static se.skl.tp.vp.wsdl.PathHelper.getPath;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import se.skl.tp.vp.constants.PropertyConstants;

@Service
@Log4j2
public class WsdlConfigurationJson implements WsdlConfiguration {

  private List<WsdlConfig> wsdlConfigs;
  private HashMap<String, WsdlConfig> mapOnTjanstekontrakt;
  private HashMap<String, WsdlConfig> mapOnWsdlUrl;

  public WsdlConfigurationJson(
      @Value("${" + PropertyConstants.WSDL_JSON_FILE + "}") String wsdlJsonFile,
      @Value("${" + PropertyConstants.WSDLFILES_DIRECTORY + "}") String wsdlFilesDirectory)
      throws IOException, URISyntaxException {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      wsdlConfigs =
          objectMapper.readValue(
              getPath(wsdlJsonFile).toFile(),
              new TypeReference<List<WsdlConfig>>() {});
    } catch (FileNotFoundException e) {
      wsdlConfigs = new ArrayList<>();
      log.warn(
          "Json file for wsdlconfiguration not found at "
              + wsdlJsonFile
              + ", unless wsdl paths are generated from base wsdl directory no wsdls are available.");
    }

    createConfigurationFromWsdlFiles(getPath(wsdlFilesDirectory).toString());

    initMaps();
  }

  private void initMaps() {
    mapOnTjanstekontrakt = new HashMap<>();
    mapOnWsdlUrl = new HashMap<>();
    for (WsdlConfig wsdlConfig : wsdlConfigs) {
      mapOnTjanstekontrakt.put(wsdlConfig.getTjanstekontrakt().toLowerCase(), wsdlConfig);
      mapOnWsdlUrl.put(wsdlConfig.getWsdlurl().toLowerCase(), wsdlConfig);
    }
  }

  private void createConfigurationFromWsdlFiles(String wsdlDirectory) {
    try (Stream<Path> paths = Files.walk(Paths.get(wsdlDirectory))) {
      paths
          .filter(f -> f.toString().endsWith(".wsdl"))
          .forEach(this::fillConfigFromWsdlFile);
    } catch (IOException e) {
      log.warn(
          "Problem when trying to read wsdl files in "
              + wsdlDirectory
              + ". No wsdl paths are automatically genereted. Message: "
              + e.toString());
    }
  }

  private void fillConfigFromWsdlFile(Path file) {
    WsdlInfo wsdlInfo = getWsdlInfoFromFile(file);
    String serviceInteractionNameSpace = wsdlInfo.getServiceInteractionNameSpace();

    if (serviceInteractionNameSpace != null) {
      String[] serviceNameSpaceArray = serviceInteractionNameSpace.split(":");
      String maindomain = serviceNameSpaceArray[2];

      int serviceNameSpaceSize = serviceNameSpaceArray.length - 1;
      String rivtaVersion = serviceNameSpaceArray[serviceNameSpaceSize];
      String serviceVersion = serviceNameSpaceArray[serviceNameSpaceSize - 1];
      String serviceName = serviceNameSpaceArray[serviceNameSpaceSize - 2];

      StringBuilder subdomainBuilder = new StringBuilder();
      subdomainBuilder.append(serviceNameSpaceArray[3]);
      int i = serviceNameSpaceSize - 3;
      for (int y = 4; i >= y; y++) {
        subdomainBuilder.append(":").append(serviceNameSpaceArray[y]);
      }
      String subdomain = subdomainBuilder.toString();

      String subdomainAdress = subdomain.replaceAll(":", "/");
      String serviceRelativePath = serviceName + "/" + serviceVersion + "/" + rivtaVersion;

      String wsdlurl = "vp/" + maindomain + "/" + subdomainAdress + "/" + serviceRelativePath;
      String wsdlPath = file.toString();
      String tjanstekontrakt = wsdlInfo.getServiceContractName();

      if (!tjantekontraktHasConfig(tjanstekontrakt)) {
        WsdlConfig wsdlConfig = new WsdlConfig();
        wsdlConfig.setWsdlurl(wsdlurl);
        wsdlConfig.setWsdlfilepath(wsdlPath);
        wsdlConfig.setTjanstekontrakt(tjanstekontrakt);
        wsdlConfigs.add(wsdlConfig);
      }
    }
  }

  private boolean tjantekontraktHasConfig(String tjanstekontrakt) {
    for (WsdlConfig wsdlConfig : wsdlConfigs) {
      if (wsdlConfig.getTjanstekontrakt().equalsIgnoreCase(tjanstekontrakt)) {
        return true;
      }
    }
    return false;
  }

  private WsdlInfo getWsdlInfoFromFile(Path file) {
    WsdlInfo wsdlInfo = new WsdlInfo();
    try (InputStream is = new FileInputStream(file.toFile())) {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document document = db.parse(is);
      Node node = document.getDocumentElement();

      NamedNodeMap attributes = node.getAttributes();
      for (int i = 0; i < attributes.getLength(); i++) {
        String attribute = attributes.item(i).getNodeValue();
        if (attribute.toLowerCase().contains("rivtabp")) {
          wsdlInfo.setServiceInteractionNameSpace(attribute);
        } else if (attribute.toLowerCase().contains("responder")) {
          wsdlInfo.setServiceContractName(attribute);
        }
      }

    } catch (IOException | SAXException | ParserConfigurationException e) {
      log.error("Error when trying to parse wsdl file " + file.toString(), e);
    }
    return wsdlInfo;
  }

  @Override
  public WsdlConfig getOnWsdlUrl(String wsdlUrl) {
    String uri =  wsdlUrl.startsWith("/") ? wsdlUrl.substring(1): wsdlUrl;
    return mapOnWsdlUrl.get(uri.toLowerCase());
  }

  @Override
  public WsdlConfig getOnTjanstekontrakt(String tjanstekontrakt) {
    return mapOnTjanstekontrakt.get(tjanstekontrakt);
  }

  @Override
  public List<String> getAllWsdlUrl() {
    return wsdlConfigs.stream().map(WsdlConfig::getWsdlurl).collect(Collectors.toList());
  }

  private class WsdlInfo {
    String serviceInteractionNameSpace;
    String ServiceContractName;

    public String getServiceInteractionNameSpace() {
      return serviceInteractionNameSpace;
    }

    public void setServiceInteractionNameSpace(String serviceInteractionNameSpace) {
      this.serviceInteractionNameSpace = serviceInteractionNameSpace;
    }

    public String getServiceContractName() {
      return ServiceContractName;
    }

    public void setServiceContractName(String serviceContractName) {
      ServiceContractName = serviceContractName;
    }
  }
}
