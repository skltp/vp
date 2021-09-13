package se.skl.tp.vp.vagval;

import java.net.URI;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.skl.tp.vagval.logging.ThreadContextLogTrace;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.service.TakCacheService;
import se.skltp.takcache.RoutingInfo;

@Service
public class VagvalProcessor implements Processor {

    @Autowired
    TakCacheService takService;

    @Autowired
    ExceptionUtil exceptionUtil;

    @Override
    public void process(Exchange exchange) throws Exception {
        String receiverId = (String) exchange.getProperty(VPExchangeProperties.RECEIVER_ID);
        String servicecontractNamespace = (String) exchange.getProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE);

        validateRequest(servicecontractNamespace, receiverId);

        List<RoutingInfo> routingInfos = takService.getRoutingInfo(servicecontractNamespace, receiverId);
        exchange.setProperty(VPExchangeProperties.VAGVAL_TRACE, ThreadContextLogTrace.get(ThreadContextLogTrace.ROUTER_RESOLVE_VAGVAL_TRACE) );

        RoutingInfo routingInfo = validateResponse(routingInfos, servicecontractNamespace, receiverId);

        exchange.setProperty(VPExchangeProperties.VAGVAL, routingInfo.getAddress() );
        exchange.setProperty(VPExchangeProperties.RIV_VERSION_OUT, routingInfo.getRivProfile() );

        URI uri = new URI(routingInfo.getAddress());
        if(uri.getPort() == -1) {
            int defaultPort = uri.getScheme()!=null &&  uri.getScheme().equalsIgnoreCase("https") ? 443 : 8080;
            exchange.setProperty(VPExchangeProperties.VAGVAL_HOST, String.format("%s:%d", uri.getHost(), defaultPort));
        } else {
            exchange.setProperty(VPExchangeProperties.VAGVAL_HOST, String.format("%s:%d", uri.getHost(), uri.getPort()));
        }
        
        if(uri.getQuery()==null || uri.getQuery().isEmpty()) {
            exchange.getIn().setHeader(Exchange.HTTP_PATH, uri.getPath());
        } else {
            exchange.getIn().setHeader(Exchange.HTTP_PATH, String.format("%s?%s",uri.getPath(), uri.getQuery()));
        }

    }

    public RoutingInfo validateResponse(List<RoutingInfo> routingInfos, String tjanstegranssnitt, String receiverAddress){

        if(routingInfos.isEmpty()){
            String whitespaceDetectedHintString="";
            if (receiverAddress.contains(" ")) {
                whitespaceDetectedHintString = ". Whitespace detected in incoming request!";
            }
            throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP004, getRequestSummaryString(tjanstegranssnitt, receiverAddress)+whitespaceDetectedHintString);
        }

        if(routingInfos.size()>1){
            throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP006, getRequestSummaryString(tjanstegranssnitt, receiverAddress));
        }

        RoutingInfo routingInfo = routingInfos.get(0);

        if (routingInfo.getAddress() == null || routingInfo.getAddress().trim().length() == 0) {
            throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP010, getRequestSummaryString(tjanstegranssnitt, receiverAddress));
        }

        return routingInfo;
    }

    private void validateRequest(String servicecontractNamespace, String receiverId) {
        if (!takService.isInitalized()) {
            throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP008);
        }

        //TODO Kontrollera servicecontractNamespace ?

        // No receiver ID (to_address) found in message
        if(receiverId == null){
            throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP003);
        }
    }

    private String getRequestSummaryString(String tjanstegranssnitt, String receiverAddress) {
        return String.format( "serviceNamespace: %s, receiverId: %s",tjanstegranssnitt, receiverAddress);
    }
}
