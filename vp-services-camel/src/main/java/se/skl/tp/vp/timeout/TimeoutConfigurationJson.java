package se.skl.tp.vp.timeout;

import static se.skl.tp.vp.wsdl.PathHelper.getPath;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.constants.PropertyConstants;

@Service
public class TimeoutConfigurationJson implements TimeoutConfiguration {

  private static Logger LOGGER = LogManager.getLogger(TimeoutConfigurationJson.class);

  private List<TimeoutConfig> timeoutConfigs;
  private HashMap<String, TimeoutConfig> mapOnTjanstekontrakt;

  public void setMapOnTjansteKontrakt(HashMap<String, TimeoutConfig> map) {
    // Used in tests
    mapOnTjanstekontrakt = map;
  }

  public TimeoutConfigurationJson(
      @Value("${" + PropertyConstants.TIMEOUT_JSON_FILE + "}") String timeoutJsonFile,
      @Value("${" + PropertyConstants.TIMEOUT_JSON_FILE_DEFAULT_TJANSTEKONTRAKT_NAME + "}")
          String timeoutJsonFileDefaultTjanstekontraktName)
      throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      timeoutConfigs = objectMapper.readValue(getPath(timeoutJsonFile).toFile(), new TypeReference<List<TimeoutConfig>>() {});
    } catch (FileNotFoundException e) {
      LOGGER.warn("Json file for timeouts not found at " + timeoutJsonFile + ".");
    } catch (JsonParseException e) {
      LOGGER.warn("Json file for timeouts " + timeoutJsonFile + " could not be parsed.");
    } catch (URISyntaxException e) {
      LOGGER.warn("Json file for timeouts failed " + timeoutJsonFile + ".", e);
    }
    if (timeoutConfigs == null) {
      timeoutConfigs = new ArrayList<>();
    }
    boolean defaultTimeoutsExist = false;
    for (TimeoutConfig timeoutConfig : timeoutConfigs) {
      if (timeoutConfig.getTjanstekontrakt().equalsIgnoreCase(timeoutJsonFileDefaultTjanstekontraktName)) {
        defaultTimeoutsExist = true;
      }
    }
    if (!defaultTimeoutsExist) {
      createDefaultTimeoutsWhenMissing(timeoutJsonFileDefaultTjanstekontraktName);
      LOGGER.warn("Could not find any default timeoutvalues, using producertimeout=29000 and routetimeout=30000 as default timeouts. " +
              "Please create and configure a timeoutconfig.json file to set this manually.");
    }

    initMaps();
  }

  private void createDefaultTimeoutsWhenMissing(String defaultTjanstekontrakt) {
    TimeoutConfig timeoutConfig = new TimeoutConfig();
    timeoutConfig.setTjanstekontrakt(defaultTjanstekontrakt);
    timeoutConfig.setProducertimeout(29000);
    timeoutConfig.setRoutetimeout(30000);
    timeoutConfigs.add(timeoutConfig);
  }

  private void initMaps() {
    mapOnTjanstekontrakt = new HashMap<>();
    for (TimeoutConfig timeoutConfig : timeoutConfigs) {
      mapOnTjanstekontrakt.put(timeoutConfig.getTjanstekontrakt(), timeoutConfig);
    }
  }

  @Override
  public TimeoutConfig getOnTjanstekontrakt(String tjanstekontrakt) {
    return mapOnTjanstekontrakt.get(tjanstekontrakt);
  }
}
