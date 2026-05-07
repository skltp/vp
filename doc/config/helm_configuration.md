# App-of-Apps Helm Configuration Guide

This guide explains how to deploy **vp-services-camel** (VP) using the **app-of-apps** pattern with ArgoCD. It is aimed at operators setting up a new environment from scratch.

For per-key documentation of all VP Helm values, see [Helm Values Reference].

---

## 1. The App-of-Apps Pattern

The SKLTP platform uses the [ArgoCD App-of-Apps](https://argo-cd.readthedocs.io/en/stable/operator-manual/cluster-bootstrapping/#app-of-apps-pattern) approach to manage multiple applications from a single Git repository.

### How It Works

You create a **central repository** (the "app-of-apps" repo) containing a Helm chart. When rendered, this chart produces an ArgoCD **ApplicationSet** resource that drives the deployment of all platform services — including VP.

The **ApplicationSet** uses a **list generator** that iterates over an `applications[]` list in `values.yaml`. For each entry it:

1. Reads `valuefiles/common-values.yaml` (shared settings: image registry, ingress hostnames, SKLTP instance ID).
2. Reads `valuefiles/<name>-values.yaml` (application-specific overrides).
3. Merges both into the `helm.values` field of the generated ArgoCD Application.
4. Points ArgoCD at the application's own Git repository + `helm/` path + pinned tag.

ArgoCD then renders each application's Helm chart (e.g. `vp/helm/`) with the merged values and syncs the resulting Kubernetes resources to the target cluster.

### Value Precedence (highest → lowest)

1. `valuefiles/vp-values.yaml` (environment-specific overrides)
2. `valuefiles/common-values.yaml` (shared across all apps)
3. `vp/helm/values.yaml` (chart defaults in the VP repository)

---

## 2. Setting Up Your App-of-Apps Repository

Create a new Git repository with the following structure:

```
my-platform-apps/
├── Chart.yaml
├── values.yaml
├── valuefiles/
│   ├── common-values.yaml
│   └── vp-values.yaml
└── templates/
    ├── applicationset.yaml
    ├── configmaps/
    │   ├── common-configmap.yaml
    │   ├── vp-configmap.yaml
    │   └── vp-trust-configmap.yaml
    └── secrets/
        └── (SealedSecrets or references)
```

### 2.1 `Chart.yaml`

```yaml
apiVersion: v2
name: my-platform-applicationset
description: App-of-apps chart for deploying SKLTP services
type: application
version: 0.1.0
appVersion: "0.0.1"
```

### 2.2 `values.yaml` — Cluster & Application List

This is the top-level values file for your app-of-apps chart. It defines the target cluster/namespace and lists which applications to deploy.

```yaml
destination:
  cluster: a                              # CHANGE: cluster identifier
  environment: myenv                      # CHANGE: environment name (dev, qa, prod, etc.)
  project: my-platform-project            # CHANGE: ArgoCD project name
  namespace: my-platform-myenv            # CHANGE: target Kubernetes namespace
  server: https://kubernetes.default.svc

repo:
  path: helm                              # Path within each app repo where Helm chart lives

applications:
- name: vp
  repourl: https://github.com/skltp/vp.git
  targetrevision: v4.7.12                 # CHANGE: pin to desired VP release tag
```

### 2.3 `templates/applicationset.yaml` — The List Generator

This is the core template that generates one ArgoCD Application per entry in `applications[]`. It merges `common-values.yaml` and the per-app values file into the Helm values for each application.

```yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: {{ .Chart.Name }}-{{ .Values.destination.environment }}
  namespace: argocd
spec:
  generators:
  - list:
      elements:
      {{- range .Values.applications }}
      - application: {{ .name }}-{{ $.Values.destination.environment }}
        repourl: {{ .repourl }}
        targetrevision: {{ .targetrevision }}
        app-values: | {{ $.Files.Get "valuefiles/common-values.yaml" | nindent 10 }}
          {{ $.Files.Get (printf "valuefiles/%s-values.yaml" .name) | nindent 10 }}
      {{- end }}
  template:
    metadata:
      name: '{{`{{application}}`}}'
    spec:
      destination:
        namespace: {{ .Values.destination.namespace }}
        server: {{ .Values.destination.server }}
      project: {{ .Values.destination.project }}
      source:
        repoURL: '{{`{{repourl}}`}}'
        path: {{ .Values.repo.path }}
        targetRevision: '{{`{{targetrevision}}`}}'
        helm:
          values: '{{`{{app-values}}`}}'
```

> **Key points about this template:**
> - It uses `$.Files.Get` to read the value files from your app-of-apps repo and inject them as inline Helm values.
> - The double-brace escaping (`{{` `` ` `` `{{...}}` `` ` `` `}}`) is required because the inner `{{application}}`, `{{repourl}}`, etc. are ArgoCD ApplicationSet template parameters — not Go template expressions.
> - Each application gets its own ArgoCD Application resource pointing at the application's own Git repo and Helm chart.

---

## 3. Minimal VP Deployment Configuration

### 3.1 `valuefiles/common-values.yaml` — Shared Values

Settings consumed by VP and potentially other SKLTP services you deploy:

```yaml
repository: registry.example.com/skltp/        # CHANGE: your container registry prefix

ingressroute:
  esbHostName: esb.myenv.example.se            # CHANGE: public ESB hostname
  sjunetHostName: esb.myenv.sjunet.org         # CHANGE: Sjunet hostname
  bksVpHostName: esb.myenv.example.se          # CHANGE: Hostname if accessing the cluster directly

skltp:
  instanceId: MY-PLATFORM_ID                   # CHANGE: ID of this VP instance
```

### 3.2 `valuefiles/vp-values.yaml` — VP-Specific Overrides

Minimum overrides for VP:

| Concern               | Keys to set                                                                             |
|-----------------------|-----------------------------------------------------------------------------------------|
| Scaling               | `deployment.replicaCount`, `deployment.resources`                                       |
| Image                 | `container.image.pullPolicy`                                                            |
| Environment variables | `environment.variables.config_maps`, `environment.variables.secrets`                    |
| TLS bundles           | `tls.bundles[].tlsSecret`, `tls.bundles[].truststoreConfigMap`                          |
| HSA cron job          | `hsaCacheCronJob.schedule`, `hsaCacheCronJob.certSecret`, `hsaCacheCronJob.trustSecret` |
| IP whitelists         | `ipWhiteList_http.sourceRange`, `ipWhiteList_direct.sourceRange`                        |

See section 4 for the full example.

### 3.3 Kubernetes Resources (Created via `templates/`)

These must exist in the target namespace before (or alongside) the VP deployment. Create them as additional templates in your app-of-apps chart:

| Resource                       | Purpose                                                            |
|--------------------------------|--------------------------------------------------------------------|
| `ConfigMap/common-configmap`   | TAK endpoint address shared across services.                       |
| `ConfigMap/vp-configmap`       | VP instance name, HSA file URL, mTLS flag, sender-ID allowed list. |
| `ConfigMap/vp-trust-configmap` | CA certificate bundle for outbound mTLS trust.                     |
| `Secret/<tls-secret>`          | Client certificate + private key for producer mTLS.                |
| `Secret/<trust-secret>`        | CA certificate used by the HSA cron job.                           |
| `Secret/regcred`               | Image-pull credentials for the container registry.                 |

> **Note:** Secrets should be provisioned via SealedSecrets, external-secrets-operator, or your organization's secret management solution. Never commit plaintext secrets to Git.

#### About `regcred` (Image-Pull Secret)

The `regcred` Secret is a Kubernetes `kubernetes.io/dockerconfigjson` secret that stores credentials for authenticating against the container image registry. Without it, the kubelet cannot pull the VP container image and pods will fail with `ErrImagePull` / `ImagePullBackOff`.

The VP Helm chart references this secret via `imagePullSecrets`:

```yaml
imagePullSecrets:
  - name: regcred
```

**Creating `regcred` manually** (for testing/bootstrapping):

```bash
kubectl create secret docker-registry regcred \
  --namespace=<your-namespace> \
  --docker-server=registry.example.com \
  --docker-username=<service-account> \
  --docker-password=<token-or-password>
```

**In production**, use SealedSecrets or an external-secrets-operator to manage this secret declaratively. The secret must exist in the same namespace as the VP Deployment. If your cluster uses a shared image-pull secret at the ServiceAccount level, you can set `imagePullSecrets: []` in the values file to skip per-pod configuration.

---

## 4. Complete Minimal App-of-Apps Example

Below is a self-contained set of all files needed in your app-of-apps repository to deploy VP. Each file is separated by `---` with a header comment.

> Replace placeholder values (marked with `# CHANGE`) with your environment-specific settings.

```yaml
##############################################################################
# FILE: Chart.yaml
##############################################################################
apiVersion: v2
name: my-platform-applicationset
description: App-of-apps chart for deploying SKLTP services
type: application
version: 0.1.0
appVersion: "0.0.1"
---
##############################################################################
# FILE: values.yaml — Cluster & application list
##############################################################################
destination:
  cluster: a                                    # CHANGE: cluster identifier
  environment: myenv                            # CHANGE: environment name
  project: my-platform-project                  # CHANGE: ArgoCD project
  namespace: my-platform-myenv                  # CHANGE: target namespace
  server: https://kubernetes.default.svc

repo:
  path: helm

applications:
- name: vp
  repourl: https://github.com/skltp/vp.git
  targetrevision: v4.7.12                      # CHANGE: desired VP version
---
##############################################################################
# FILE: templates/applicationset.yaml — The list generator
##############################################################################
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: {{ .Chart.Name }}-{{ .Values.destination.environment }}
  namespace: argocd
spec:
  generators:
  - list:
      elements:
      {{- range .Values.applications }}
      - application: {{ .name }}-{{ $.Values.destination.environment }}
        repourl: {{ .repourl }}
        targetrevision: {{ .targetrevision }}
        app-values: | {{ $.Files.Get "valuefiles/common-values.yaml" | nindent 10 }}
          {{ $.Files.Get (printf "valuefiles/%s-values.yaml" .name) | nindent 10 }}
      {{- end }}
  template:
    metadata:
      name: '{{`{{application}}`}}'
    spec:
      destination:
        namespace: {{ .Values.destination.namespace }}
        server: {{ .Values.destination.server }}
      project: {{ .Values.destination.project }}
      source:
        repoURL: '{{`{{repourl}}`}}'
        path: {{ .Values.repo.path }}
        targetRevision: '{{`{{targetrevision}}`}}'
        helm:
          values: '{{`{{app-values}}`}}'
---
##############################################################################
# FILE: valuefiles/common-values.yaml — Shared values for all applications
##############################################################################
repository: registry.example.com/skltp/         # CHANGE: your registry prefix

ingressroute:
  esbHostName: esb.myenv.example.se            # CHANGE
  sjunetHostName: esb.myenv.sjunet.org         # CHANGE
  bksVpHostName: esb.myenv.example.se          # CHANGE

skltp:
  instanceId: MY-PLATFORM_ID                    # CHANGE: ID of this VP instance
---
##############################################################################
# FILE: valuefiles/vp-values.yaml — VP-specific overrides
##############################################################################
deployment:
  replicaCount: 2
  elasticGrokFilter: camel
  resources:
    limits:
      memory: 2Gi
    requests:
      cpu: 200m
      memory: 2Gi
  topologySpreadConstraints:
    - labelSelector:
        matchLabels:
          app: vp
      maxSkew: 2
      topologyKey: kubernetes.io/hostname
      whenUnsatisfiable: DoNotSchedule

container:
  image:
    pullPolicy: IfNotPresent

environment:
  variables:
    config_maps:
      - common-configmap
      - vp-configmap
    secrets: []

tls:
  bundles:
    - name: clientcert
      tlsSecret: myenv-vp-tls                   # CHANGE: TLS Secret name
      truststoreConfigMap: vp-trust-configmap

hsaCacheCronJob:
  schedule: "25 1 * * *"
  image:
    tag: "1.1.1"
    pullPolicy: IfNotPresent
  certSecret: myenv-vp-tls                      # CHANGE
  trustSecret: vp-ca-cert                       # CHANGE

ipWhiteList_http:
  sourceRange:
    - 10.0.0.10                                 # CHANGE: reverse-proxy IP(s)
  ipStrategy:
    depth: 1

ipWhiteList_liveness:
  sourceRange:
    - 10.0.0.0/8
  ipStrategy:
    depth: 1

ipWhiteList_direct:
  sourceRange:
    - 10.6.3.0/24                               # CHANGE: VPN/admin subnet

log4j:
  rootLoggerLevel: WARN
  loggers:
    - name: se.skl.tp
      level: WARN
    - name: se.skl.takcache
      level: INFO
    - name: se.skl.tp.vp.service.HsaCacheServiceImpl
      level: INFO
    - name: se.skl.tp.vp.VpServicesApplication
      level: INFO
    - name: se.skl.tp.vp.logging.req.in
      level: INFO
    - name: se.skl.tp.vp.logging.resp.out
      level: INFO
---
##############################################################################
# FILE: templates/configmaps/common-configmap.yaml
##############################################################################
apiVersion: v1
kind: ConfigMap
metadata:
  name: common-configmap
  namespace: {{ .Values.destination.namespace }}
data:
  TAKCACHE_ENDPOINT_ADDRESS: "http://tak-services-svc:8080/tak-services/SokVagvalsInfo/v2"
---
##############################################################################
# FILE: templates/configmaps/vp-configmap.yaml
##############################################################################
apiVersion: v1
kind: ConfigMap
metadata:
  name: vp-configmap
  namespace: {{ .Values.destination.namespace }}
data:
  VP_INSTANCE_NAME: "MY-PLATFORM-MYENV"          # CHANGE
  HSA_FILE_URL: "https://hsa-mtls.katalog.sjunet.org/hsafileservice/hsaunits.zip"  # CHANGE
  HSA_SEND_ALERT_MAIL: "false"
  VP_TLS_MTLS_VERIFICATION_ENABLED: "true"
  SENDER_ID_ALLOWED_LIST: >
    MY-PLATFORM_HSAID
---
##############################################################################
# FILE: templates/configmaps/vp-trust-configmap.yaml
##############################################################################
apiVersion: v1
kind: ConfigMap
metadata:
  name: vp-trust-configmap
  namespace: {{ .Values.destination.namespace }}
data:
  ca.crt: |
    # Paste your trusted CA certificate chain here (PEM format).
    # Include the CA certificates required by your producers
    # (e.g. SITHS e-id Root CA v2, SITHS e-id Function CA v1).
    -----BEGIN CERTIFICATE-----
    <YOUR-CA-CERTIFICATE>
    -----END CERTIFICATE-----
---
##############################################################################
# FILE: templates/secrets/regcred.yaml (placeholder — use SealedSecret)
##############################################################################
# apiVersion: bitnami.com/v1alpha1
# kind: SealedSecret
# metadata:
#   name: regcred
#   namespace: {{ .Values.destination.namespace }}
# spec:
#   encryptedData:
#     .dockerconfigjson: <sealed-value>
#   template:
#     type: kubernetes.io/dockerconfigjson
---
##############################################################################
# FILE: templates/secrets/vp-tls.yaml (placeholder — use SealedSecret)
##############################################################################
# apiVersion: bitnami.com/v1alpha1
# kind: SealedSecret
# metadata:
#   name: myenv-vp-tls
#   namespace: {{ .Values.destination.namespace }}
# spec:
#   encryptedData:
#     tls.crt: <sealed-value>
#     tls.key: <sealed-value>
#   template:
#     type: kubernetes.io/tls
```

---

## 5. Deployment Workflow

1. **Create your app-of-apps repository** — Use the structure and files from section 4.
2. **Provision secrets** — Create SealedSecrets (or use your secrets operator) for TLS certificates, image-pull credentials, and CA bundles.
3. **Register in ArgoCD** — Create an ArgoCD Application that points at your app-of-apps repository (the "root" application). ArgoCD will render the chart, producing the ApplicationSet.
4. **Sync** — ArgoCD detects the ApplicationSet, generates one Application per entry in `applications[]`, renders each app's Helm chart with the merged values, and applies the resources to the cluster.
5. **Verify** — Check pod status, Actuator health (`/actuator/health`), and TAK cache initialisation logs.

### Registering the Root Application in ArgoCD

The "root application" is the single ArgoCD Application that bootstraps everything else. It tells ArgoCD where your app-of-apps repository lives and how to render it. Without this, ArgoCD has no knowledge of your chart.

You can create the root application declaratively or via the ArgoCD UI/CLI:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: my-platform-apps
  namespace: argocd
spec:
  project: my-platform-project                                       # CHANGE: must exist in ArgoCD
  source:
    repoURL: https://git.example.com/my-org/my-platform-apps.git     # CHANGE: your app-of-apps repo
    path: .                                                          # Chart.yaml is at the repo root
    targetRevision: main                                             # CHANGE: branch or tag to track
  destination:
    server: https://kubernetes.default.svc                           # The cluster ArgoCD runs on
    namespace: argocd                                                # ApplicationSet is created here
  syncPolicy:
    automated:
      prune: true       # Remove resources ArgoCD no longer manages
      selfHeal: true    # Revert manual drift automatically
```

Once this root Application is synced, ArgoCD renders your `Chart.yaml` + `values.yaml` + templates, producing the ApplicationSet which in turn creates the VP Application (and any other applications you list).

---

## 6. Additional Override Examples

### 6.1 Resource Limits

```yaml
deployment:
  resources:
    limits:
      memory: 3Gi
    requests:
      cpu: 500m
      memory: 3Gi
```

### 6.2 Log Levels

```yaml
log4j:
  rootLoggerLevel: WARN
  loggers:
    - name: se.skl.tp
      level: WARN
    - name: se.skl.tp.vp.logging.req.in
      level: INFO
    - name: se.skl.tp.vp.logging.resp.out
      level: INFO
    - name: org.apache.camel
      level: WARN
```

### 6.3 Per-Contract Timeouts

```yaml
timeoutConfig:
  - tjanstekontrakt: default_timeouts
    routetimeout: 35000
    producertimeout: 35000
  - tjanstekontrakt: urn:riv:clinicalprocess:healthcond:actoutcome:GetCareDocumentationResponder:2
    routetimeout: 60000
    producertimeout: 55000
```

### 6.4 TLS Cipher-Suite Override for a Specific Producer

```yaml
tls:
  overrides:
    - name: legacy-producer
      bundle: clientcert
      match:
        domainSuffix: legacy.producer.example.org
      protocolsInclude:
        - TLSv1.2
      cipherSuitesInclude:
        - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
        - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
```

### 6.5 Sender-ID Allowed List

```yaml
senderIdAllowedList:
  - HSASERVICES-106J
  - SE162321000180-0018
  - SE162321000198-001J
```

### 6.6 IP Whitelists

```yaml
ipWhiteList_http:
  sourceRange:
    - 10.252.10.40   # reverse-proxy node 1
    - 10.252.10.41   # reverse-proxy node 2
  ipStrategy:
    depth: 1

ipWhiteList_direct:
  sourceRange:
    - 10.6.3.0/24    # VPN subnet for direct access
```

### 6.7 Ingress mTLS Configuration

```yaml
ingress:
  tls:
    secret:
      name: myenv.esb.example.se
    mtls:
      clientAuth:
        secretNames:
          - siths-ca-bundle
        clientAuthType: RequireAndVerifyClientCert
```

---

## See Also

- [Helm Values Reference] — complete per-key documentation of `helm/values.yaml`.
- [VP Camel konfigurering] — application-level properties (`application.properties`, `application-security.properties`).
- [Loggning konfiguration] — Log4j2 configuration and ECS format.

[//]: # (Reference links)

[Helm Values Reference]: <helm_values.md>
[VP Camel konfigurering]: <configuration.md>
[Loggning konfiguration]: <logging_configuration.md>

