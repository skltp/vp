package se.skl.tp.vp.actuator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import se.skl.tp.vp.service.TakCacheService;
import se.skltp.takcache.TakCacheLog;

import static org.junit.jupiter.api.Assertions.*;

class TakCacheHealthIndicatorTest {

    @Mock
    TakCacheService takCacheServiceMock;

    TakCacheHealthIndicator indicator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        indicator = new TakCacheHealthIndicator(takCacheServiceMock);
    }

    @Test
    void testNotInitialized() {
        Health health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void testInitializedOk() {
        TakCacheLog log = new TakCacheLog();
        log.setRefreshStatus(TakCacheLog.RefreshStatus.REFRESH_OK);
        Mockito.when(takCacheServiceMock.isInitalized()).thenReturn(true);
        Mockito.when(takCacheServiceMock.getLastRefreshLog()).thenReturn(log);

        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    void testRefreshFailed() {
        TakCacheLog log = new TakCacheLog();
        log.setRefreshStatus(TakCacheLog.RefreshStatus.REFRESH_FAILED);
        Mockito.when(takCacheServiceMock.isInitalized()).thenReturn(false);
        Mockito.when(takCacheServiceMock.getLastRefreshLog()).thenReturn(log);

        Health health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
    }
}