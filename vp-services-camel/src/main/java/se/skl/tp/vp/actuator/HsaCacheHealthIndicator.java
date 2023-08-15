package se.skl.tp.vp.actuator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.service.HsaCacheService;
import se.skl.tp.vp.service.HsaCacheStatus;

@Component
public class HsaCacheHealthIndicator implements HealthIndicator {

    HsaCacheService hsaCacheService;

    @Autowired
    public HsaCacheHealthIndicator(HsaCacheService hsaCacheService) {
        this.hsaCacheService = hsaCacheService;
    }

    @Override
    public Health health() {
        HsaCacheStatus hsaCacheStatus = hsaCacheService.getHsaCacheStatus();
        if (hsaCacheStatus == null || !hsaCacheStatus.isInitialized()) {
            return Health.down().withDetail("hsaCacheStatus", "Not initialized").build();
        }
        return Health.up().build();
    }
}