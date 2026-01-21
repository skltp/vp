package se.skl.tp.vp.logging.logentry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * System properties initialized once at startup for use in ECS logging.
 * All fields are nullable and will be null if the information cannot be determined.
 */
@Slf4j
@Getter
public class EcsSystemProperties {

    private static final EcsSystemProperties INSTANCE = new EcsSystemProperties();

    // Constants for system properties and environment variables
    private static final String OS_ARCH = "os.arch";
    private static final String OS_NAME = "os.name";
    private static final String OS_VERSION = "os.version";
    private static final String ENV_CONTAINER = "container";
    private static final String ENV_KUBERNETES = "KUBERNETES_SERVICE_HOST";

    // OS family and platform constants
    private static final String WINDOWS = "windows";
    private static final String UNIX = "unix";
    private static final String DARWIN = "darwin";
    private static final String LINUX = "linux";
    private static final String SOLARIS = "solaris";
    private static final String MAC = "mac";
    private static final String SUNOS = "sunos";

    private final String hostName;
    private final String hostIp;
    private final String hostArchitecture;
    private final String hostOsFamily;
    private final String hostOsName;
    private final String hostOsVersion;
    private final String hostOsPlatform;
    private final String hostType;

    EcsSystemProperties() {
        this.hostName = determineHostName();
        this.hostIp = determineHostIp();
        this.hostArchitecture = System.getProperty(OS_ARCH);
        this.hostOsName = System.getProperty(OS_NAME);
        this.hostOsVersion = System.getProperty(OS_VERSION);
        this.hostOsFamily = determineOsFamily(this.hostOsName);
        this.hostOsPlatform = determineOsPlatform(this.hostOsName);
        this.hostType = determineHostType();
    }

    String determineHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            log.warn("Failed to get host name.", e);
            return null;
        }
    }

    String determineHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Failed to get host IP.", e);
            return null;
        }
    }

    String determineOsFamily(String osName) {
        if (osName == null) return null;
        String lowerOsName = osName.toLowerCase();
        if (lowerOsName.contains(WINDOWS)) return WINDOWS;
        if (lowerOsName.contains(LINUX) || lowerOsName.contains(UNIX)) return UNIX;
        if (lowerOsName.contains(DARWIN) || lowerOsName.contains(MAC)) return DARWIN;
        if (lowerOsName.contains(SOLARIS) || lowerOsName.contains(SUNOS)) return UNIX;
        return null;
    }

    String determineOsPlatform(String osName) {
        if (osName == null) return null;
        String lowerOsName = osName.toLowerCase();
        if (lowerOsName.contains(WINDOWS)) return WINDOWS;
        if (lowerOsName.contains(LINUX)) return LINUX;
        if (lowerOsName.contains(DARWIN) || lowerOsName.contains(MAC)) return DARWIN;
        if (lowerOsName.contains(SOLARIS) || lowerOsName.contains(SUNOS)) return SOLARIS;
        if (lowerOsName.contains(UNIX)) return UNIX;
        return null;
    }

    String determineHostType() {
        if (System.getenv(ENV_CONTAINER) != null || System.getenv(ENV_KUBERNETES) != null) {
            return ENV_CONTAINER;
        }
        return null;
    }

    /**
     * Get the singleton instance of EcsSystemProperties.
     * @return the singleton instance
     */
    public static EcsSystemProperties getInstance() {
        return INSTANCE;
    }
}
