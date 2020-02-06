package se.skl.tp.vp.vagval;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.skl.tp.vagval.logging.ThreadContextLogTrace;
import se.skl.tp.vp.constants.VPExchangeProperties;
import se.skl.tp.vp.errorhandling.ExceptionUtil;
import se.skl.tp.vp.exceptions.VpSemanticErrorCodeEnum;
import se.skl.tp.vp.service.TakCacheService;


@Service
public class BehorighetProcessor implements Processor {

    @Autowired
    TakCacheService takService;

    @Autowired
    ExceptionUtil exceptionUtil;

    @Override
    public void process(Exchange exchange) throws Exception {
        if (!takService.isInitalized()) {
            throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP008);
        }

        String receiverId = (String) exchange.getProperty(VPExchangeProperties.RECEIVER_ID);
        String senderId = (String) exchange.getProperty(VPExchangeProperties.SENDER_ID);
        String servicecontractNamespace = (String) exchange.getProperty(VPExchangeProperties.SERVICECONTRACT_NAMESPACE);

        validateRequest(senderId, receiverId, servicecontractNamespace);

        boolean isAuthorized = takService.isAuthorized(senderId, servicecontractNamespace, receiverId);
        exchange.setProperty(VPExchangeProperties.ANROPSBEHORIGHET_TRACE, ThreadContextLogTrace.get(ThreadContextLogTrace.ROUTER_RESOLVE_ANROPSBEHORIGHET_TRACE) );
        if( !isAuthorized ){
            throw exceptionUtil.createVpSemanticException( VpSemanticErrorCodeEnum.VP007, getRequestSummaryString(servicecontractNamespace, receiverId, senderId));
        }
    }

    private void validateRequest(String senderId, String receiverId, String servicecontractNamespace) {
        //TODO Kontrollera servicecontractNamespace ?

        // No sender ID (from_address) found in certificate
        if(senderId == null){
            throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP002, getRequestSummaryString(servicecontractNamespace, receiverId, senderId));
        }

        // No receiver ID (to_address) found in message
        if(receiverId == null){
            throw exceptionUtil.createVpSemanticException(VpSemanticErrorCodeEnum.VP003, getRequestSummaryString(servicecontractNamespace, receiverId, senderId));
        }

    }

    private String getRequestSummaryString(String serviceNamespace, String logicalAddress, String senderId) {
        return String.format( "serviceNamespace: %s, receiverId: %s, senderId: %s", serviceNamespace, logicalAddress, senderId);
    }

}
