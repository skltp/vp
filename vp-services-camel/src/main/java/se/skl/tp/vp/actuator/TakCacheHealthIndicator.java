package se.skl.tp.vp.actuator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import se.skl.tp.vp.service.TakCacheService;
import se.skltp.takcache.TakCacheLog;

@Component
public class TakCacheHealthIndicator implements HealthIndicator {

    TakCacheService takCacheService;

    @Autowired
    public TakCacheHealthIndicator(TakCacheService takCacheService) {
        this.takCacheService = takCacheService;
    }

    @Override
    public Health health() {
        if (!takCacheService.isInitalized()) {
            return Health.down().withDetail("refreshStatus", getTakRefreshStatus()).build();
        }
        return Health.up().build();
    }

    private String getTakRefreshStatus() {
        TakCacheLog takCacheLog = takCacheService.getLastRefreshLog();
        return takCacheLog == null ? "Not initialized" : takCacheLog.getRefreshStatus().toString();
    }
}