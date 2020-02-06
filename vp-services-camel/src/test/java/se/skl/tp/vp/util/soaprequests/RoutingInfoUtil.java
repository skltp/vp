package se.skl.tp.vp.util.soaprequests;

import se.skltp.takcache.RoutingInfo;

public class RoutingInfoUtil {
  public static RoutingInfo createRoutingInfo(String address, String rivProfile){
    RoutingInfo routingInfo = new RoutingInfo();
    routingInfo.setAddress(address);
    routingInfo.setRivProfile(rivProfile);
    return routingInfo;
  }

}
