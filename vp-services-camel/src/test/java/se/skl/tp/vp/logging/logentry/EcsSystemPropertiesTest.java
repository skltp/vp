package se.skl.tp.vp.logging.logentry;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EcsSystemPropertiesTest {

    @Test
    void testGetInstance_returnsSameInstance() {
        EcsSystemProperties instance1 = EcsSystemProperties.getInstance();
        EcsSystemProperties instance2 = EcsSystemProperties.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    @Test
    void testDetermineHostName_success() {
        InetAddress mockAddress = mock(InetAddress.class);
        when(mockAddress.getCanonicalHostName()).thenReturn("test-host.example.com");

        try (MockedStatic<InetAddress> mockedInetAddress = Mockito.mockStatic(InetAddress.class)) {
            mockedInetAddress.when(InetAddress::getLocalHost).thenReturn(mockAddress);

            EcsSystemProperties props = new EcsSystemProperties();
            String hostName = props.determineHostName();

            assertEquals("test-host.example.com", hostName);
        }
    }

    @Test
    void testDetermineHostName_unknownHostException() {
        try (MockedStatic<InetAddress> mockedInetAddress = Mockito.mockStatic(InetAddress.class)) {
            mockedInetAddress.when(InetAddress::getLocalHost).thenThrow(new UnknownHostException());

            EcsSystemProperties props = new EcsSystemProperties();
            String hostName = props.determineHostName();

            assertNull(hostName);
        }
    }

    @Test
    void testDetermineHostIp_success() {
        InetAddress mockAddress = mock(InetAddress.class);
        when(mockAddress.getHostAddress()).thenReturn("192.168.1.100");

        try (MockedStatic<InetAddress> mockedInetAddress = Mockito.mockStatic(InetAddress.class)) {
            mockedInetAddress.when(InetAddress::getLocalHost).thenReturn(mockAddress);

            EcsSystemProperties props = new EcsSystemProperties();
            String hostIp = props.determineHostIp();

            assertEquals("192.168.1.100", hostIp);
        }
    }

    @Test
    void testDetermineHostIp_unknownHostException() {
        try (MockedStatic<InetAddress> mockedInetAddress = Mockito.mockStatic(InetAddress.class)) {
            mockedInetAddress.when(InetAddress::getLocalHost).thenThrow(new UnknownHostException());

            EcsSystemProperties props = new EcsSystemProperties();
            String hostIp = props.determineHostIp();

            assertNull(hostIp);
        }
    }

    @Test
    void testDetermineOsFamily_windows() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertEquals("windows", props.determineOsFamily("Windows 10"));
        assertEquals("windows", props.determineOsFamily("Windows Server 2019"));
        assertEquals("windows", props.determineOsFamily("WINDOWS NT"));
    }

    @Test
    void testDetermineOsFamily_unix() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertEquals("unix", props.determineOsFamily("Linux"));
        assertEquals("unix", props.determineOsFamily("Unix"));
        assertEquals("unix", props.determineOsFamily("SunOS"));
        assertEquals("unix", props.determineOsFamily("Solaris"));
    }

    @Test
    void testDetermineOsFamily_darwin() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertEquals("darwin", props.determineOsFamily("Mac OS X"));
        assertEquals("darwin", props.determineOsFamily("Darwin"));
    }

    @Test
    void testDetermineOsFamily_null() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertNull(props.determineOsFamily(null));
    }

    @Test
    void testDetermineOsFamily_unknown() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertNull(props.determineOsFamily("Unknown OS"));
    }

    @Test
    void testDetermineOsPlatform_windows() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertEquals("windows", props.determineOsPlatform("Windows 10"));
        assertEquals("windows", props.determineOsPlatform("Windows Server 2019"));
    }

    @Test
    void testDetermineOsPlatform_linux() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertEquals("linux", props.determineOsPlatform("Linux"));
    }

    @Test
    void testDetermineOsPlatform_darwin() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertEquals("darwin", props.determineOsPlatform("Mac OS X"));
        assertEquals("darwin", props.determineOsPlatform("Darwin"));
    }

    @Test
    void testDetermineOsPlatform_solaris() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertEquals("solaris", props.determineOsPlatform("SunOS"));
        assertEquals("solaris", props.determineOsPlatform("Solaris"));
    }

    @Test
    void testDetermineOsPlatform_unix() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertEquals("unix", props.determineOsPlatform("Unix"));
    }

    @Test
    void testDetermineOsPlatform_null() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        assertNull(props.determineOsPlatform(null));
    }

    @Test
    void testDetermineHostType_callsSystemGetenv() {
        // This test just verifies the method can be called
        // We cannot mock System.getenv due to Mockito limitations
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        // Just verify the method runs without error
        String hostType = props.determineHostType();
        // Result depends on runtime environment - can be "container" or null
        assertTrue(hostType == null || "container".equals(hostType));
    }

    @Test
    void testGetters_returnInitializedValues() {
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        // Verify all getters return non-null or expected values
        assertNotNull(props);

        // Architecture should always be available from System properties
        assertNotNull(props.getHostArchitecture());

        // OS name should always be available from System properties
        assertNotNull(props.getHostOsName());

        // OS version should always be available from System properties
        assertNotNull(props.getHostOsVersion());

        // Derived values should be deterministic based on OS name
        assertNotNull(props.getHostOsFamily());
        assertNotNull(props.getHostOsPlatform());

        // Host name and IP may be null if network is unavailable
        // but should be non-null in most environments
        // (not asserting as it depends on runtime environment)

        // Host type may be null if not running in a container
        // (not asserting as it depends on runtime environment)
    }

    @Test
    void testOsFamilyConsistency() {
        // Verify that OS family determination is consistent
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        String actualOsName = System.getProperty("os.name");
        String determinedFamily = props.determineOsFamily(actualOsName);

        // Should return the same result when called multiple times
        assertEquals(determinedFamily, props.determineOsFamily(actualOsName));
        assertEquals(determinedFamily, props.getHostOsFamily());
    }

    @Test
    void testOsPlatformConsistency() {
        // Verify that OS platform determination is consistent
        EcsSystemProperties props = EcsSystemProperties.getInstance();

        String actualOsName = System.getProperty("os.name");
        String determinedPlatform = props.determineOsPlatform(actualOsName);

        // Should return the same result when called multiple times
        assertEquals(determinedPlatform, props.determineOsPlatform(actualOsName));
        assertEquals(determinedPlatform, props.getHostOsPlatform());
    }
}
