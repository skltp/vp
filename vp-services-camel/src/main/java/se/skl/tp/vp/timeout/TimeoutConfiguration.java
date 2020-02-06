package se.skl.tp.vp.timeout;

import java.util.HashMap;

public interface TimeoutConfiguration {
    TimeoutConfig getOnTjanstekontrakt(String tjanstekontrakt);
    void setMapOnTjansteKontrakt(HashMap<String, TimeoutConfig> map);
}
