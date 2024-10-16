package se.skl.tp.vp;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.impl.event.CamelContextStartedEvent;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.service.HsaCacheService;
import se.skl.tp.vp.service.TakCacheService;

@Slf4j
@Component
public class VPStartupEventNotifier extends EventNotifierSupport {
    private final HsaCacheService hsaCacheService;
    private final TakCacheService takCacheService;

    @Autowired
    public VPStartupEventNotifier( HsaCacheService hsaCacheService, TakCacheService takCacheService) {
        this.hsaCacheService = hsaCacheService;
        this.takCacheService = takCacheService;
    }

    @Override
    protected void doStart() throws Exception {
        setIgnoreCamelContextEvents(false);

        // filter out unwanted events
        setIgnoreExchangeSentEvents(true);
        setIgnoreExchangeCompletedEvent(true);
        setIgnoreExchangeFailedEvents(true);
        setIgnoreServiceEvents(true);
        setIgnoreRouteEvents(true);
        setIgnoreExchangeCreatedEvent(true);
        setIgnoreExchangeRedeliveryEvents(true);
    }

    private void initHSACache() {
        hsaCacheService.resetCache();
    }
    private void initTakCache() {
        takCacheService.refresh();
    }

	@Override
	public void notify(CamelEvent event) throws Exception {
        if (event instanceof CamelContextStartedEvent) {
            initTakCache();
            initHSACache();
        }
		
	}
}