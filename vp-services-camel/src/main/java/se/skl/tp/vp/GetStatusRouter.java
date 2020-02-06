package se.skl.tp.vp;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.status.GetStatusProcessor;

@Component
public class GetStatusRouter extends RouteBuilder {

    public static final String HTTP_GET = "get-status";

    public static final String NETTY4_HTTP_GET = "netty4-http:{{vp.status.url}}"
        + "?chunkedMaxContentLength={{vp.max.receive.length}}";

    @Autowired
    GetStatusProcessor getStatusProcessor;

    @Override
    public void configure() throws Exception {
        from(NETTY4_HTTP_GET).routeId(HTTP_GET)
                .process(getStatusProcessor);
    }
}
