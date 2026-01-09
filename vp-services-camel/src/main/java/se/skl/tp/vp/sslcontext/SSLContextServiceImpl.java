package se.skl.tp.vp.sslcontext;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;
import se.skl.tp.vp.config.TLSProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SSLContextServiceImpl implements SSLContextService {

    private final Map<String, String> sslContextMap = new ConcurrentHashMap<>();
    private final TLSProperties tlsProperties;

    public SSLContextServiceImpl(TLSProperties tlsProperties) {
        this.tlsProperties = tlsProperties;
    }

    @Override
    public String getClientSSLContextId(String vagvalHost) {
        return sslContextMap.computeIfAbsent(vagvalHost, k -> {
            String overrideId = getOverrideId(vagvalHost);
            if (overrideId != null) {
                return overrideId;
            }
            return getDefaultId();
        });
    }

    private record DomainNameAndPort(String domainName, int port) {}

    private @Nullable String getOverrideId(String vagvalHost) {
        DomainNameAndPort domainNameAndPort = null;
        if (tlsProperties.getOverrides() != null) {
            for (var override : tlsProperties.getOverrides()) {
                if (override.getMatch() == null || override.getName() == null) {
                    log.warn("Ignoring TLS override without match or name: {}", override);
                    continue;
                }
                var match = override.getMatch();
                if (domainNameAndPort == null) {
                    domainNameAndPort = getDomainNameAndPort(vagvalHost);
                }
                boolean matches = matchFound(match, domainNameAndPort);
                if (matches) {
                    return SSLContextParametersConfig.getId(override.getName());
                }
            }
        }
        return null;
    }

    private static boolean matchFound(TLSProperties.TLSConfigMatch match, DomainNameAndPort domainNameAndPort) {
        boolean domainMatches = isDomainMatch(match, domainNameAndPort.domainName);
        boolean portMatches = isPortMatch(match, domainNameAndPort.port);
        return domainMatches && portMatches;
    }

    private static boolean isDomainMatch(TLSProperties.TLSConfigMatch match, String domainName) {
        if (match.getDomainName() != null) {
            return match.getDomainName().equalsIgnoreCase(domainName);
        }
        if (match.getDomainSuffix() != null) {
            return domainName.toLowerCase().endsWith(match.getDomainSuffix().toLowerCase());
        }
        return false;
    }

    private static boolean isPortMatch(TLSProperties.TLSConfigMatch match, int port) {
        return match.getPort() == null || match.getPort() == port;
    }

    private static @NonNull DomainNameAndPort getDomainNameAndPort(String vagvalHost) {
        DomainNameAndPort domainNameAndPort;
        String[] hostParts = vagvalHost.split(":");
        if (hostParts.length > 1) {
            domainNameAndPort = new DomainNameAndPort(hostParts[0], Integer.parseInt(hostParts[1]));
        } else {
            domainNameAndPort = new DomainNameAndPort(hostParts[0], 443);
        }
        return domainNameAndPort;
    }

    private @NonNull String getDefaultId() {
        if (tlsProperties.getDefaultConfig() == null) {
            return SSLContextParametersConfig.getId(SSLContextParametersConfig.DEPRECATED_CONTEXT);
        }
        if (tlsProperties.getDefaultConfig().getName() == null) {
            throw new IllegalStateException("Default TLS configuration must have a name");
        }
        return SSLContextParametersConfig.getId(tlsProperties.getDefaultConfig().getName());
    }
}
