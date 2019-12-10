package se.skl.tp.vp.wsdl;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class WsdlConfig {

  private String tjanstekontrakt;
  private String wsdlfilepath;
  private String wsdlurl;

}
