package se.skl.tp.vp.timeout;

public class TimeoutConfig {

    private String tjanstekontrakt;
    private int routetimeout;
    private int producertimeout;

    public String getTjanstekontrakt() {
        return tjanstekontrakt;
    }

    public void setTjanstekontrakt(String tjanstekontrakt) {
        this.tjanstekontrakt = tjanstekontrakt;
    }

    public int getRoutetimeout() {
        return routetimeout;
    }

    public void setRoutetimeout(int routetimeout) {
        this.routetimeout = routetimeout;
    }

    public int getProducertimeout() {
        return producertimeout;
    }

    public void setProducertimeout(int producertimeout) {
        this.producertimeout = producertimeout;
    }
}
