package se.skl.tp.vp.integrationtests.utils;

import java.io.File;
import java.net.URL;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.ws.Endpoint;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import se.skltp.tak.vagvalsinfo.wsdl.v2.AnropsBehorighetsInfoType;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;

@Log4j2
@Service
@Profile("StartTakService")
public class TakMockWebService {

  private Endpoint endpoint;
  private String url;

  SokVagvalsServiceSoap11LitDoc sokVagvalsInfo;

  @XmlRootElement(name = "persistentCache")
  public static class VirtuliseringCache {

    @XmlElement
    protected List<VirtualiseringsInfoType> virtualiseringsInfo;
  }

  @XmlRootElement(name = "persistentCache")
  public static class BehorighetCache {

    @XmlElement
    protected List<AnropsBehorighetsInfoType> anropsBehorighetsInfo;
  }


  public TakMockWebService(@Value("${takcache.endpoint.address}") String url) {
    sokVagvalsInfo = new SokVagvalsServiceSoap11LitDoc();
    setUrl(url);
    setBehorigheterFromXmlResource("takdata/tak-behorigheter-test.xml");
    setVagvalFromXmlResource("takdata/tak-vagval-test.xml");
  }

  @PostConstruct
  public void postConstruct() {
    start();
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setBehorigheterFromXmlResource(String resourceName) {
    URL url = TakMockWebService.class.getClassLoader().getResource(resourceName);
    setAnropsBehorigheterResult(restoreFromLocalCache(url.getFile(), BehorighetCache.class));
  }

  public void setVagvalFromXmlResource(String resourceName) {
    URL url = TakMockWebService.class.getClassLoader().getResource(resourceName);
    setVirtualiseringarResult(restoreFromLocalCache(url.getFile(), VirtuliseringCache.class));
  }

  public void start() {
    if (!isStarted()) {
      log.info("Starting TakMockWebService  at: {}", url);
      endpoint = Endpoint.publish(url, sokVagvalsInfo);
    } else {
      log.warn("TakMockWebService is already started.");
    }
  }

  @PreDestroy
  public void stop() {
    if (endpoint != null) {
      endpoint.stop();
      endpoint = null;
    }
  }

  public boolean isStarted() {
    return endpoint != null && endpoint.isPublished();
  }

  public void setVirtualiseringarResult(VirtuliseringCache virtualiseringar) {
    sokVagvalsInfo.hamtaAllaVirtualiseringar(null).getVirtualiseringsInfo().clear();
    sokVagvalsInfo.hamtaAllaVirtualiseringar(null).getVirtualiseringsInfo()
        .addAll(virtualiseringar.virtualiseringsInfo);
  }

  public void setAnropsBehorigheterResult(BehorighetCache anropsBehorigheter) {
    sokVagvalsInfo.hamtaAllaAnropsBehorigheter(null).getAnropsBehorighetsInfo().clear();
    sokVagvalsInfo.hamtaAllaAnropsBehorigheter(null).getAnropsBehorighetsInfo()
        .addAll(anropsBehorigheter.anropsBehorighetsInfo);
  }


  public static <T> T restoreFromLocalCache(String fileName, Class<T> className) {
    Unmarshaller jaxbUnmarshaller = null;
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(className);
      jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      return (T) jaxbUnmarshaller.unmarshal(new File(fileName));
    } catch (JAXBException e) {
      log.error(e);
      return null;
    }
  }


}
