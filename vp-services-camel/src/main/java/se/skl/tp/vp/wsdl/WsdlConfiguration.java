package se.skl.tp.vp.wsdl;

import java.util.List;

public interface WsdlConfiguration {

  WsdlConfig getOnWsdlUrl(String wsdlUrl);

  WsdlConfig getOnTjanstekontrakt(String tjanstekontrakt);

  List<String> getAllWsdlUrl();
}
