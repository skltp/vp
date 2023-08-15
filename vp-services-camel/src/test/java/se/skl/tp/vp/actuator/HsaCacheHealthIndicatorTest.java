package se.skl.tp.vp.actuator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import se.skl.tp.vp.service.HsaCacheService;
import se.skl.tp.vp.service.HsaCacheStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HsaCacheHealthIndicatorTest {

    @Mock
    HsaCacheService hsaCacheServiceMock;

    HsaCacheHealthIndicator indicator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        indicator = new HsaCacheHealthIndicator(hsaCacheServiceMock);
    }

    @Test
    void testNotInitialized() {
        Health health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void testInitializedOk() {
        HsaCacheStatus status = new HsaCacheStatus();
        status.setInitialized(true);
        Mockito.when(hsaCacheServiceMock.getHsaCacheStatus()).thenReturn(status);

        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus());
    }
}