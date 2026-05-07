# Helm Values Reference

This page documents every configurable value in `helm/values.yaml` for the VP (VirtualiseringsPlattformen) Helm chart.
For application-level property documentation, see [VP Camel konfigurering]. For logging configuration, see [Loggning konfiguration].

---

## namespace

| Key         | Description                                                 |
|-------------|-------------------------------------------------------------|
| `namespace` | Kubernetes namespace where all chart resources are created. |

---

## deployment

General deployment settings for the VP pod.

| Key                                    | Description                                                                                                                                                                             |
|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `deployment.name`                      | Name used for the Deployment resource and related labels.                                                                                                                               |
| `deployment.elasticGrokFilter`         | Value injected as a label/annotation for Elastic log pipeline grok-filter matching.                                                                                                     |
| `deployment.replicaCount`              | Number of pod replicas to run.                                                                                                                                                          |
| `deployment.resources`                 | Kubernetes resource requests and limits (`cpu`, `memory`). Set to `{}` to omit. Structure follows the standard `requests.cpu`, `requests.memory`, `limits.cpu`, `limits.memory` format. |
| `deployment.topologySpreadConstraints` | List of Kubernetes [topology spread constraints](https://kubernetes.io/docs/concepts/scheduling-eviction/topology-spread-constraints/) to distribute pods across nodes.                 |

---

## skltp

Core SKLTP / VP instance settings.

| Key                        | Description                                                                                             |
|----------------------------|---------------------------------------------------------------------------------------------------------|
| `skltp.instanceId`         | Unique identifier for this VP instance. Maps to `vp.instance.id`.                                       |
| `skltp.instanceName`       | Human-readable name for this VP instance. Used in error messages. Maps to `vp.instance.name`.           |
| `skltp.maxReceiveLength`   | Maximum allowed size (bytes) for an incoming HTTP response body. Maps to `vp.maxreceive.length`.        |
| `skltp.useRoutingHistory`  | Enable round-trip detection via the `x-rivta-routing-history` header. Maps to `vp.use.routing.history`. |
| `skltp.urls.hsaResetCache` | Listener URL for the HSA cache reset endpoint. Maps to `vp.hsa.reset.cache.url`.                        |
| `skltp.urls.httpRoute`     | Listener URL for inbound plaintext HTTP traffic. Maps to `vp.http.route.url`.                           |
| `skltp.urls.httpsRoute`    | Listener URL for inbound mTLS HTTPS traffic. Maps to `vp.https.route.url`.                              |
| `skltp.urls.resetCache`    | Listener URL for the TAK cache reset endpoint. Maps to `vp.reset.cache.url`.                            |
| `skltp.urls.status`        | Listener URL for the VP status service. Maps to `vp.status.url`.                                        |

---

## Miscellaneous Top-Level Settings

| Key                                          | Description                                                                                                                                          |
|----------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `memoryLoggerPeriodSeconds`                  | Interval in seconds between JVM memory usage log entries.                                                                                            |
| `propagateCorrelationIdForHttps`             | Whether the correlation ID is forwarded on outbound HTTPS calls. Maps to `propagate.correlation.id.for.https`.                                       |
| `messageLoggerMethod`                        | Message logging strategy. `ecs` uses the ECS JSON format; `legacy` uses the older string format. Maps to `vp.logging.style`.                         |
| `throwVp013WhenOriginalconsumerNotAllowed`   | When `true`, VP returns SOAP fault VP013 if the sender is not on `senderIdAllowedList`. When `false`, a warning is logged and the request continues. |
| `timeoutJsonFileDefaultTjanstekontraktName`  | Name of the service contract entry in `timeoutConfig` whose timeouts are used as defaults.                                                           |
| `vagvalrouterDefaultRoutingAddressDelimiter` | Delimiter character that activates default routing (VG#VE) during *vägval* and *behörighet* evaluation. Empty string disables default routing.       |

---

## server

Embedded management server (Undertow) settings — serves Actuator endpoints.

| Key                                | Description                                                                                                                                        |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| `server.forwardHeadersStrategy`    | Spring Boot forwarded-headers strategy (`NONE`, `NATIVE`, `FRAMEWORK`). Controls how `X-Forwarded-*` headers are handled by the management server. |
| `server.port`                      | Port for the management / Actuator HTTP server.                                                                                                    |
| `server.undertow.accesslogDir`     | Directory for Undertow access log files. Typically a template expression referencing `paths.log`.                                                  |
| `server.undertow.accesslogEnabled` | Enable or disable Undertow access logging.                                                                                                         |
| `server.undertow.accesslogPattern` | Access log format pattern (e.g. `common`, `combined`).                                                                                             |
| `server.undertow.accesslogPrefix`  | Filename prefix for access log files.                                                                                                              |
| `server.undertow.accesslogRotate`  | Enable daily rotation of access log files.                                                                                                         |
| `server.undertow.accesslogSuffix`  | Filename suffix for access log files.                                                                                                              |

---

## producer

Netty client-side settings for outbound connections to service producers.

| Key                       | Description                                                                                                     |
|---------------------------|-----------------------------------------------------------------------------------------------------------------|
| `producer.httpKeepalive`  | Enable HTTP keep-alive on outbound plaintext connections to producers.                                          |
| `producer.httpWorkers`    | Number of Netty event-loop threads for the HTTP producer. `0` lets Netty auto-detect based on available cores.  |
| `producer.httpsKeepalive` | Enable HTTP keep-alive on outbound TLS connections to producers.                                                |
| `producer.httpsWorkers`   | Number of Netty event-loop threads for the HTTPS producer. `0` lets Netty auto-detect based on available cores. |

---

## takcache

Connection settings for the TAK (Tjänsteadresseringskatalogen) cache.

| Key                           | Description                                                                                                          |
|-------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `takcache.endpointAddress`    | URL of the TAK web service used to populate the local *vägval* and *behörighet* cache. **Override per environment.** |
| `takcache.persistentFileName` | File path for the persisted local TAK cache. Used as fallback when the remote TAK service is unreachable.            |

---

## hsacache

HSA (national organisational directory) cache settings and the associated cron-job download configuration.

| Key                           | Description                                                                                                             |
|-------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| `hsacache.fileName`           | Name of the HSA cache file on disk.                                                                                     |
| `hsacache.fileUrl`            | Remote URL from which the HSA organisational-unit archive is downloaded. **Override per environment.**                  |
| `hsacache.caCert`             | Path (inside the cron-job container) to the CA certificate used when downloading the HSA file over TLS.                 |
| `hsacache.tlsCert`            | Path to the client TLS certificate used for the HSA file download.                                                      |
| `hsacache.tlsKey`             | Path to the client TLS private key used for the HSA file download.                                                      |
| `hsacache.resetLabelSelector` | Kubernetes label selector used by the cron job to discover VP pods and trigger an HSA cache reset.                      |
| `hsacache.resetUrlFormat`     | `printf`-style URL template for the HSA cache reset endpoint. `%s` is replaced with each pod IP.                        |
| `hsacache.fileAllowableDiff`  | Maximum allowed difference (number of entries) between the new and current HSA file. Prevents accidental mass-deletion. |
| `hsacache.sendAlertMail`      | Whether to send an alert e-mail when the HSA cache refresh fails.                                                       |

---

## javaOpts

| Key        | Description                                                                                                                                                                                                                                                                                                                  |
|------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `javaOpts` | Complete `JAVA_OPTS` string passed to the JVM. Contains all system properties, heap settings, JMX flags, and the Log4j2 configuration file path. The value is processed through Helm's `tpl` function, so Go template expressions (e.g. `{{ tpl .Values.paths.config . }}`) are evaluated. Override the entire string per environment as needed. |

---

## See Also

HTTP header filtering applied to outbound requests toward producers.

| Key                               | Description                                                                                                                             |
|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `headers.requestHeadersToRemove`  | List of header-name patterns (regex fragments) removed from outbound requests. Prevents leaking internal or proxy headers to producers. |
| `headers.forwardedHeaderAuthCert` | Name of the inbound HTTP header that carries the client certificate forwarded by a TLS-terminating proxy (e.g. Traefik).                |

---

## management

Spring Boot Actuator / management endpoint exposure.

| Key                                      | Description                                                                                 |
|------------------------------------------|---------------------------------------------------------------------------------------------|
| `management.endpointsWebExposureInclude` | List of Actuator endpoint IDs to expose over HTTP (e.g. `health`, `metrics`, `prometheus`). |
| `management.endpointPrometheusEnabled`   | Enable the Prometheus metrics scrape endpoint.                                              |

---

## senderIdAllowedList

| Key                   | Description                                                                                                                                                                                     |
|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `senderIdAllowedList` | List of HSA-IDs allowed to set the `x-rivta-original-serviceconsumer-hsaid` header. An empty list means all senders are allowed. Used together with `throwVp013WhenOriginalconsumerNotAllowed`. |

---

## repository

| Key          | Description                                                                                                                                                                                                                  |
|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `repository` | Container image registry prefix (e.g. `registry.example.com/org/`). Prepended to `container.image.name` and `hsaCacheCronJob.image.name` when constructing the full image reference. **Must be overridden per environment.** |

---

## container

| Key                          | Description                                                                                     |
|------------------------------|-------------------------------------------------------------------------------------------------|
| `container.image.name`       | Container image name (without registry/tag).                                                    |
| `container.image.tag`        | Image tag / version for the VP container. Defaults to the Helm chart `appVersion` when not set. |
| `container.image.pullPolicy` | Kubernetes image pull policy (`Always`, `IfNotPresent`, `Never`).                               |

---

## Scheduling

Optional Kubernetes scheduling constraints for the VP pod. These are not set in `values.yaml` by default.

| Key            | Description                                                                                                                                                               |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `nodeSelector` | Map of node labels the VP pod must be scheduled on. See [Kubernetes nodeSelector](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#nodeselector). |
| `affinity`     | Kubernetes [affinity/anti-affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity) rules for the VP pod.            |
| `tolerations`  | List of Kubernetes [tolerations](https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/) allowing the VP pod to schedule on tainted nodes.         |

---

## imagePullSecrets

| Key                | Description                                                                                                                          |
|--------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `imagePullSecrets` | List of Kubernetes Secret references used to authenticate against the container image registry. Each entry must have a `name` field. |

---

## hsaCacheCronJob

Kubernetes CronJob that periodically downloads the HSA organisational-unit file and triggers cache resets in VP pods.

| Key                                 | Description                                                                                       |
|-------------------------------------|---------------------------------------------------------------------------------------------------|
| `hsaCacheCronJob.elasticGrokFilter` | Grok filter label for log pipeline matching of cron-job logs.                                     |
| `hsaCacheCronJob.schedule`          | Cron expression defining when the HSA cache job runs.                                             |
| `hsaCacheCronJob.timeZone`          | IANA time zone for the cron schedule (requires Kubernetes ≥ 1.27).                                |
| `hsaCacheCronJob.image.name`        | Container image name for the HSA cache job.                                                       |
| `hsaCacheCronJob.image.tag`         | Image tag / version for the HSA cache job.                                                        |
| `hsaCacheCronJob.image.pullPolicy`  | Image pull policy for the HSA cache job.                                                          |
| `hsaCacheCronJob.certSecret`        | Name of the Kubernetes TLS Secret containing the client certificate and key used by the cron job. |
| `hsaCacheCronJob.trustSecret`       | Name of the Kubernetes Secret containing the CA certificate trusted by the cron job.              |

---

## ingressroute

Traefik IngressRoute hostnames. **All values must be overridden per environment.**

| Key                           | Description                                                                                        |
|-------------------------------|----------------------------------------------------------------------------------------------------|
| `ingressroute.esbHostName`    | Public hostname for the ESB ingress (e.g. `esb.prod.ntjp.se`). Used in the HTTP IngressRoute rule. |
| `ingressroute.sjunetHostName` | Public hostname for the Sjunet ingress. Used in the HTTP IngressRoute rule.                        |
| `ingressroute.bksVpHostName`  | Public hostname for direct HTTPS access (mTLS). Used in the direct IngressRoute rule.              |

---

## ingress

TLS and mTLS settings applied at the ingress level (Traefik).

| Key                                          | Description                                                                                                                                                          |
|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ingress.enabled`                            | Enable or disable ingress resources.                                                                                                                                 |
| `ingress.tls.secret.name`                    | Name of the Kubernetes TLS Secret used for the server certificate.                                                                                                   |
| `ingress.tls.version.min`                    | Minimum TLS version accepted by the ingress (e.g. `VersionTLS12`).                                                                                                   |
| `ingress.tls.version.max`                    | Maximum TLS version accepted by the ingress (e.g. `VersionTLS13`).                                                                                                   |
| `ingress.tls.mtls.clientAuth.secretNames`    | List of Kubernetes Secret names containing CA certificates trusted for client (mTLS) authentication. Rendered directly into the Traefik TLSOption `clientAuth` spec. |
| `ingress.tls.mtls.clientAuth.clientAuthType` | Traefik client-auth mode: `RequireAndVerifyClientCert`, `VerifyClientCertIfGiven`, `NoClientCert`. Rendered directly into the Traefik TLSOption `clientAuth` spec.   |

---

## ipWhiteList_http

IP whitelist applied to the HTTP ingress route (plaintext proxy traffic).

| Key                                 | Description                                                                      |
|-------------------------------------|----------------------------------------------------------------------------------|
| `ipWhiteList_http.sourceRange`      | List of allowed CIDR ranges. **Override to restrict to your reverse-proxy IPs.** |
| `ipWhiteList_http.ipStrategy.depth` | Number of `X-Forwarded-For` hops to trust when determining the client IP.        |

---

## ipWhiteList_liveness

IP whitelist for health-check / liveness probe traffic.

| Key                                     | Description                                                                      |
|-----------------------------------------|----------------------------------------------------------------------------------|
| `ipWhiteList_liveness.sourceRange`      | List of allowed CIDR ranges. **Override to restrict to your load-balancer IPs.** |
| `ipWhiteList_liveness.ipStrategy.depth` | Number of forwarded hops to trust.                                               |

---

## ipWhiteList_direct

IP whitelist for direct HTTPS access (bypassing the proxy). The entire object is rendered as the Traefik `ipWhiteList` middleware spec, so any valid Traefik `ipWhiteList` fields (e.g. `ipStrategy.depth`) may be added.

| Key                                   | Description                                                                                             |
|---------------------------------------|---------------------------------------------------------------------------------------------------------|
| `ipWhiteList_direct.sourceRange`      | List of allowed CIDR ranges. Default permits all (`0.0.0.0/0`). **Override to restrict direct access.** |
| `ipWhiteList_direct.ipStrategy.depth` | *(Optional)* Number of `X-Forwarded-For` hops to trust when determining the client IP.                  |

---

## ipWhiteList_vp

Application-level IP whitelist controlling which source IPs may set the `sender-id` header on HTTP calls.

| Key                       | Description                                                                                                                           |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `ipWhiteList_vp.prefixes` | List of IP address prefixes. A request's source IP must start with one of these prefixes to be allowed to set the `sender-id` header. |

---

## service

Kubernetes Service resource for VP.

| Key            | Description                                             |
|----------------|---------------------------------------------------------|
| `service.name` | Name of the Kubernetes Service.                         |
| `service.type` | Service type (`ClusterIP`, `NodePort`, `LoadBalancer`). |
| `service.port` | Port exposed by the Service (HTTP).                     |

---

## paths

File-system paths inside the VP container. Several values use Go template expressions that reference other path values.

| Key              | Description                                                                              |
|------------------|------------------------------------------------------------------------------------------|
| `paths.base`     | Base directory for VP configuration and certificates.                                    |
| `paths.data`     | Directory for runtime data files (e.g. persisted TAK cache).                             |
| `paths.hsafiles` | Directory where HSA cache files are stored. Typically backed by a PersistentVolumeClaim. |
| `paths.config`   | Configuration file directory. Defaults to `<paths.base>/config`.                         |
| `paths.log`      | Directory for application log files.                                                     |
| `paths.certs`    | Directory for TLS certificate and key files. Defaults to `<paths.base>/certs`.           |

---

## environment

ConfigMap, Secret, and volume-mount definitions injected into the VP container.

### environment.config_files

A list of config-file volume mounts. Each entry creates a volume from a ConfigMap and mounts it into the container.

| Key                                                | Description                                                      |
|----------------------------------------------------|------------------------------------------------------------------|
| `environment.config_files[].config_map`            | Name of the Kubernetes ConfigMap containing configuration files. |
| `environment.config_files[].path`                  | Mount path inside the container.                                 |
| `environment.config_files[].path_relative_to_base` | If `true`, the path is resolved relative to `paths.base`.        |
| `environment.config_files[].volume_name`           | Name of the Kubernetes volume (must be unique within the pod).   |

### environment.variables

Environment variables injected into the container from ConfigMaps and Secrets.

| Key                                          | Description                                                                                         |
|----------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `environment.variables._default_config_maps` | List of ConfigMap names whose keys are injected as environment variables by default.                |
| `environment.variables.config_maps`          | Additional ConfigMap names to inject. Override per environment.                                     |
| `environment.variables.secrets`              | Kubernetes Secret names whose keys are injected as environment variables. Override per environment. |

---

## log4j

Log4j2 logger configuration rendered into the ConfigMap-based `log4j2.xml`.

| Key                     | Description                                                                                                                                                                                     |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `log4j.loggers`         | List of logger entries. Each entry has a `name` (logger name / package) and a `level` (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `OFF`). See [Loggning konfiguration] for recommended loggers. |
| `log4j.rootLoggerLevel` | Log level for the root logger.                                                                                                                                                                  |

---

## timeoutConfig

List of per-service-contract timeout settings loaded as `timeoutconfig.json`.

| Key                               | Description                                                                                                              |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `timeoutConfig[].tjanstekontrakt` | Service-contract name or the default key (must match `timeoutJsonFileDefaultTjanstekontraktName` for the default entry). |
| `timeoutConfig[].routetimeout`    | Total Camel route timeout in milliseconds (includes network round-trip and producer processing).                         |
| `timeoutConfig[].producertimeout` | Netty producer read-timeout in milliseconds (time waiting for the producer to start responding).                         |

---

## tls

Outbound TLS configuration for connections from VP to service producers. Defines SSL bundles and cipher-suite policies.

### tls.bundles

List of named TLS bundles, each referencing Kubernetes Secrets for certificate material.

| Key                                 | Description                                                                           |
|-------------------------------------|---------------------------------------------------------------------------------------|
| `tls.bundles[].name`                | Unique name of the TLS bundle. Referenced by `tls.defaultConfig.bundle` or overrides. |
| `tls.bundles[].tlsSecret`           | Name of the Kubernetes TLS Secret containing the client certificate and private key.  |
| `tls.bundles[].truststoreConfigMap` | Name of the ConfigMap containing trusted CA certificates.                             |

### tls.defaultConfig

Default TLS settings applied to all outbound HTTPS connections unless an override matches.

| Key                                     | Description                                                                                                                                                     |
|-----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `tls.defaultConfig.name`                | Unique name for the default TLS configuration.                                                                                                                  |
| `tls.defaultConfig.bundle`              | Name of the TLS bundle to use by default. Must match a `tls.bundles[].name`.                                                                                    |
| `tls.defaultConfig.protocolsInclude`    | List of allowed TLS protocol versions (whitelist). When set, only these protocols are enabled.                                                                  |
| `tls.defaultConfig.protocolsExclude`    | List of TLS protocol versions to disallow (blacklist). Mutually exclusive with `protocolsInclude`.                                                              |
| `tls.defaultConfig.cipherSuitesInclude` | List of allowed cipher suites (whitelist). When set, only these ciphers are enabled. See [VP Camel konfigurering] for details on include vs. exclude semantics. |
| `tls.defaultConfig.cipherSuitesExclude` | List of cipher suites to disallow (blacklist). Mutually exclusive with `cipherSuitesInclude`.                                                                   |

### tls.overrides

List of TLS configuration overrides for specific producer targets. Each override can match on domain name, domain suffix, and/or port. When an outbound connection matches an override, its settings take precedence over `tls.defaultConfig`. Empty by default (`[]`). See [VP Camel konfigurering] for matching rules and examples.

| Key                                   | Description                                                                             |
|---------------------------------------|-----------------------------------------------------------------------------------------|
| `tls.overrides[].name`                | Unique name for this override entry.                                                    |
| `tls.overrides[].bundle`              | Name of the TLS bundle to use. Must match a `tls.bundles[].name`.                       |
| `tls.overrides[].match.domainName`    | Exact hostname to match for the target producer.                                        |
| `tls.overrides[].match.domainSuffix`  | Domain suffix to match (e.g. `.example.com`). Matches any host ending with this suffix. |
| `tls.overrides[].match.port`          | Port number to match for the target producer.                                           |
| `tls.overrides[].protocolsInclude`    | List of allowed TLS protocol versions (whitelist) for matched targets.                  |
| `tls.overrides[].protocolsExclude`    | List of TLS protocol versions to disallow (blacklist) for matched targets.              |
| `tls.overrides[].cipherSuitesInclude` | List of allowed cipher suites (whitelist) for matched targets.                          |
| `tls.overrides[].cipherSuitesExclude` | List of cipher suites to disallow (blacklist) for matched targets.                      |

---

## probes

Kubernetes health probes for the VP container.

### probes.startupProbe

| Key                                       | Description                                                                |
|-------------------------------------------|----------------------------------------------------------------------------|
| `probes.startupProbe.httpGet.path`        | HTTP path to probe (Actuator readiness endpoint).                          |
| `probes.startupProbe.httpGet.port`        | Named or numeric port to probe.                                            |
| `probes.startupProbe.httpGet.scheme`      | Protocol scheme (`HTTP` or `HTTPS`).                                       |
| `probes.startupProbe.initialDelaySeconds` | Seconds to wait before the first probe after container start.              |
| `probes.startupProbe.failureThreshold`    | Number of consecutive failures before the container is restarted.          |
| `probes.startupProbe.periodSeconds`       | Seconds between probe attempts.                                            |
| `probes.startupProbe.successThreshold`    | Number of consecutive successes required to mark the container as started. |
| `probes.startupProbe.timeoutSeconds`      | Seconds before a single probe attempt times out.                           |

### probes.livenessProbe

| Key                                        | Description                                          |
|--------------------------------------------|------------------------------------------------------|
| `probes.livenessProbe.httpGet.path`        | HTTP path to probe (Actuator liveness endpoint).     |
| `probes.livenessProbe.httpGet.port`        | Named or numeric port to probe.                      |
| `probes.livenessProbe.httpGet.scheme`      | Protocol scheme (`HTTP` or `HTTPS`).                 |
| `probes.livenessProbe.initialDelaySeconds` | Seconds to wait before the first liveness probe.     |
| `probes.livenessProbe.failureThreshold`    | Consecutive failures before the container is killed. |
| `probes.livenessProbe.periodSeconds`       | Seconds between liveness probes.                     |
| `probes.livenessProbe.successThreshold`    | Consecutive successes to clear a failed state.       |
| `probes.livenessProbe.timeoutSeconds`      | Seconds before a probe attempt times out.            |

---

## See Also

- [VP Camel konfigurering] — application-level properties (`application.properties`, `application-security.properties`, `timeoutconfig.json`).
- [Loggning konfiguration] — Log4j2 configuration, ECS format, and message logging.
- [Detaljerad konfiguration] — proxy setup, Hawtio authentication, example files.

[//]: # (Reference links)

[VP Camel konfigurering]: <configuration.md>
[Loggning konfiguration]: <logging_configuration.md>
[Detaljerad konfiguration]: <detail_config.md>

