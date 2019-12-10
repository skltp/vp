package se.skl.tp.vp.integrationtests.httpheader;

import org.apache.camel.Exchange;

/**
 * Implement this interface to receive the Exchange received by a org.apache.camel.Processor
 * as is just before the Process event. (For this to happen that processor of course have to
 * implement means to set/add one ore more listener and call these before beginning the process)
 */
public interface RouteProcessEventListener {

     /**
      *
      * @param exchange the Exchange object received by the Process event
      */
     void OnBeforeProcess(final Exchange exchange);
}
