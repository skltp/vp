# Migreringsguide - Loggning från LEGACY till ECS

## Översikt

Detta dokument beskriver hur man migrerar från det äldre LEGACY-loggformatet till det nya ECS (Elastic Common Schema) loggformatet i VP-tjänsten.

## Bakgrund

VP-tjänsten har historiskt använt ett proprietärt loggformat men har nu implementerat stöd för ECS-formatet, som är en branschstandard utvecklad av Elastic. ECS möjliggör:
- **Standardiserad loggning** - Enhetligt format över olika system och verktyg
- **Bättre sökbarhet** - Strukturerade fält underlättar sökning och analys
- **Integrering med moderna verktyg** - Stöd för Elasticsearch, Kibana, Logstash med flera
- **Framtidssäker lösning** - Aktivt underhållen standard med brett stöd

## Aktivera ECS-formatet

För att aktivera ECS-loggformatet, lägg till följande i `application.properties`:

```properties
vp.logging.style=ECS
```

## Fältmappning: LEGACY → ECS

LEGACY-formatet loggar i ett textformat där varje loggpost består av en start- och slutmarkering med metadata och extra information däremellan. Formatet ser ut så här:

```
skltp-messages
** {logEventName}.start ***********************************************************
LogMessage={logMessage}
ServiceImpl={serviceImplementation}
Host={hostName} ({hostIp})
ComponentId={componentId}
Endpoint={endpoint}
MessageId={messageId}
BusinessCorrelationId={businessCorrelationId}
ExtraInfo={extraInfoString}{stackTrace}
** {logEventName}.end *************************************************************
```

ECS-formatet loggar istället i JSON-format med strukturerade fält enligt Elastic Common Schema. Se [ECS Loggformat](ecs_log_format.md) för detaljerad beskrivning av fält och struktur.

### Detaljerad fältmappning

| LEGACY-sträng             | ECS-fält                                                                                                | Beskrivning                                                        |
|---------------------------|---------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| `{logEventName}`          | [log.level](https://www.elastic.co/guide/en/ecs/current/ecs-log.html#field-log-level)                   | Loggnivå.                                                          |
| `{logMessage}`            | [event.action](https://www.elastic.co/guide/en/ecs/current/ecs-event.html#field-event-action)           | Typ av händelse (t.ex. "req-in")                                   |
| `{serviceImplementation}` | [labels.route](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                         | Intern route-identifierare (t.ex. "vp-https-route")                |
| `{hostName}`              | [host.hostname](https://www.elastic.co/guide/en/ecs/current/ecs-host.html#field-host-hostname)          | Värdmaskinens namn                                                 |
| `{hostIp}`                | [host.ip](https://www.elastic.co/guide/en/ecs/current/ecs-host.html#field-host-ip)                      | IP-adress för värdmaskinen                                         |
| `{componentId}`           | [service.name](https://www.elastic.co/guide/en/ecs/current/ecs-service.html#field-service-name)         | Tjänstens namn (t.ex. "vp-services")                               |
| `{endpoint}`              | [url.full](https://www.elastic.co/guide/en/ecs/current/ecs-url.html#field-url-full)                     | Komplett URL för den inkommande requesten till tjänsten            |
| `{messageId}`             | [transaction.id](https://www.elastic.co/docs/reference/ecs/ecs-tracing#field-transaction-id)            | Identifierar den Camel exchange som används under hela operationen |
| `{businessCorrelationId}` | [trace.id](https://www.elastic.co/guide/en/ecs/current/ecs-tracing.html#field-trace-id)                 | Korrelations-ID för spårning som kan spänna över flera system      |
| `Stacktrace={stackTrace}` | [error.stack_trace](https://www.elastic.co/guide/en/ecs/current/ecs-error.html#field-error-stack-trace) | Stack trace vid exception                                          |

### ExtraInfo → ECS-fält

Fält från `ExtraInfo`-mappningen i LEGACY-formatet mappar till ECS-standardfält när det är möjligt.

| LEGACY ExtraInfo-nyckel                    | ECS-fält                                                                                                                                                                                                                                      | Beskrivning                                                                |
|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| `senderIpAdress`                           | [source.address](https://www.elastic.co/guide/en/ecs/current/ecs-source.html#field-source-address) och [source.ip](https://www.elastic.co/guide/en/ecs/current/ecs-source.html#field-source-ip)                                               | Källadress                                                                 |
| `endpoint_url`                             | [url.original](https://www.elastic.co/guide/en/ecs/current/ecs-url.html#field-url-original)                                                                                                                                                   | Komplett URL för den utgående requesten till backend (vägval)              |
| `Headers`                                  | [http.request.headers](https://doc.wikimedia.org/ecs/#field-http-request-headers) eller [http.response.headers](https://doc.wikimedia.org/ecs/#field-http-response-headers)                                                                   | HTTP request/response headers som JSON-objekt (känsliga headers filtreras) |
| `message.length`                           | [http.request.body.bytes](https://www.elastic.co/guide/en/ecs/current/ecs-http.html#field-http-request-body-bytes) eller [http.response.body.bytes](https://www.elastic.co/guide/en/ecs/current/ecs-http.html#field-http-response-body-bytes) | Storlek på request/response body i bytes                                   |
| `time.elapsed` och `time.producer`         | [event.duration](https://www.elastic.co/guide/en/ecs/current/ecs-event.html#field-event-duration)                                                                                                                                             | Tidsåtgång. OBS event.duration har enheten nanosekunder!                   |

Se kapitel `Tracing-fält` i [ECS Loggformat](ecs_log_format.md) för mer information om tolkning av `event.duration`.

### ExtraInfo → Labels

Många fält i `ExtraInfo` är domänspecifika och har ingen direkt motsvarighet i ECS-standardfält. Dessa mappas istället till [labels](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels) i ECS, vilket är avsett för anpassade fält.

| LEGACY ExtraInfo-nyckel            | ECS-fält                                  | Beskrivning                                                                                                                                                                        |
|------------------------------------|-------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `senderid`                         | [labels.senderid](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                         | HSA-ID för avsändaren av meddelandet                                                                                                                                               |
| `receiverid`                       | [labels.receiverid](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                       | HSA-ID för mottagaren av meddelandet                                                                                                                                               |
| `originalServiceconsumerHsaid`     | [labels.originalServiceconsumerHsaid](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)     | Ursprungligt HSA-ID (utgående trafik)                                                                                                                                              |
| `originalServiceconsumerHsaid_in`  | [labels.originalServiceconsumerHsaid_in](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)  | Ursprungligt HSA-ID (inkommande trafik)                                                                                                                                            |
| `actingOnBehalfOfHsaid`            | [labels.actingOnBehalfOfHsaid](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)            | HSA-ID för ursprunglig tjänstekomponent (från `x-rivta-acting-on-behalf-of-hsa-id` header)                                                                                         |
| `servicecontract_namespace`        | [labels.servicecontract_namespace](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)        | [Tjänsteinteraktionens target namespace](https://inera.atlassian.net/wiki/spaces/RTA/pages/3632903/RIV+Tekniska+Anvisningar+Tj+nsteschema#Regel-#3,-namn-p%C3%A5-target-namespace) |
| `rivversion`                       | [labels.rivversion](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                       | RIV-version/profilkortnamn (t.ex. "RIVTABP21")                                                                                                                                     |
| `wsdl_namespace`                   | [labels.wsdl_namespace](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                   | [WSDL target namespace](https://inera.atlassian.net/wiki/spaces/RTA/pages/3632875/RIV+Tekniska+Anvisningar+Basic+Profile+2.1#Regel-#4,-namn-p%C3%A5-target-namespace)              |
| `httpXForwardedProto`              | [labels.httpXForwardedProto](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)              | Protokoll från X-Forwarded-Proto header                                                                                                                                            |
| `httpXForwardedHost`               | [labels.httpXForwardedHost](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)               | Host från X-Forwarded-Host header                                                                                                                                                  |
| `httpXForwardedPort`               | [labels.httpXForwardedPort](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)               | Port från X-Forwarded-Port header                                                                                                                                                  |
| `routerVagvalTrace`                | [labels.routerVagvalTrace](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                | Spårningsinformation från vägval-routing                                                                                                                                           |
| `routerBehorighetTrace`            | [labels.routerBehorighetTrace](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)            | Spårningsinformation från anropsbehörighetskontroll                                                                                                                                |
| `sessionStatus`                    | [labels.sessionStatus](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                    | Sätts till `"true"` vid fel under processningen i VP                                                                                                                               |
| `sessionErrorDescription`          | [labels.sessionErrorDescription](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)          | Beskrivning av felet                                                                                                                                                               |
| `sessionErrorTechnicalDescription` | [labels.sessionErrorTechnicalDescription](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels) | Teknisk beskrivning av felet (exception.toString())                                                                                                                                |
| `errorCode`                        | [labels.errorCode](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                        | VP-specifik felkod                                                                                                                                                                 |
| `statusCode`                       | [labels.statusCode](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                       | HTTP-statuskod och beskrivning                                                                                                                                                     |
| `faultCode`                        | [labels.faultCode](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                        | SOAP fault code från tjänsteproducenten                                                                                                                                            |
| `faultString`                      | [labels.faultString](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                      | SOAP fault string från tjänsteproducenten                                                                                                                                          |
| `faultDetail`                      | [labels.faultDetail](https://www.elastic.co/docs/reference/ecs/ecs-base#field-labels)                      | SOAP fault detail från tjänsteproducenten                                                                                                                                          |

### Payload

Både LEGACY och ECS loggar meddelandets payload (body) endast vid **DEBUG**-loggnivå:

- **LEGACY**: `Payload`
- **ECS**: [http.request.body.content](https://www.elastic.co/guide/en/ecs/current/ecs-http.html#field-http-request-body-content) eller [http.response.body.content](https://www.elastic.co/guide/en/ecs/current/ecs-http.html#field-http-response-body-content)

Vid **INFO**-loggnivå loggas endast metadata, inte själva meddelandeinnehållet.

## Exempel: Loggpost före och efter

### LEGACY-format (req-in, INFO-nivå)

```
skltp-messages
** logEvent-info.start ***********************************************************
LogMessage=req-in
ServiceImpl=vp-https-route
Host=HOSTNAME.inera.se (192.168.1.2)
ComponentId=vp-services-test
Endpoint=https: //localhost:1028/vp/PATH1
MessageId=591E98EBD490843-0000000000000004
BusinessCorrelationId=bd411a8a-4d60-496c-9bf8-9dc551fd011d
ExtraInfo=
-originalServiceconsumerHsaid_in=originalid
-senderIpAdress=192.168.1.3
-servicecontract_namespace=urn:riv:insuranceprocess:healthreporting:GetCertificateResponder: 1
-senderid=tp
-receiverid=HttpProducer
-Headers={CamelNettyRemoteAddress=/127.0.0.1: 51265, content-length=582, CamelHttpUrl=https: //localhost:1028/vp/PATH1, CamelHttpPort=443, CamelNettyLocalAddress=/127.0.0.1:1028, CamelHttpScheme=null, CamelHttpRawQuery=null, CamelNettyChannelHandlerContext=ChannelHandlerContext(handler, [id: 0xda2487bd, L:/127.0.0.1:1028 - R:/127.0.0.1:51265]), CamelNettySSLClientCertNotAfter=Fri Feb 26 13:26:14 CET 2038, CamelNettySSLClientCertSubjectName=CN=client, OU=tp, O=SKL, L=Unknown, ST=GBG, C=SE, CamelNettySSLClientCertSerialNumber=17770512983148295944, x-rivta-original-serviceconsumer-hsaid=originalid, CamelNettySSLClientCertNotBefore=Mon Mar 04 13:26:14 CET 2013, CamelHttpMethod=POST, host=localhost:1028, CamelHttpQuery=null, connection=keep-alive, CamelHttpUri=/vp/PATH1 , CamelNettySSLSession=Session(1771427368888|TLS_DHE_DSS_WITH_AES_128_CBC_SHA256), CamelHttpHost=null, CamelHttpPath=/PATH1, CamelNettySSLClientCertIssuerName=CN=TheCA, OU=NOT FOR PRODUCTION, O=SKL, ST=GBG, C=SE}
-message.length=582
-wsdl_namespace=urn:riv:insuranceprocess:healthreporting:GetCertificate: 1:rivtabp20
-time.elapsed=2
-originalServiceconsumerHsaid=originalid
-source=se.skl.tp.vp.logging.old.LegacyMessageInfoLogger
-rivversion=rivtabp20
** logEvent-info.end *************************************************************
```

### ECS-format (req-in, INFO-nivå)

```json
{
    "@timestamp": "2026-02-18T15:17:04.657Z",
    "log.level": "INFO",
    "event.action": "req-in",
    "event.category": "[\"web\"]",
    "event.id": "04498956-6b90-41d7-bec8-e73b8d5e7537",
    "event.kind": "event",
    "event.module": "skltp-messages",
    "event.type": "[\"access\",\"start\"]",
    "host.architecture": "amd64",
    "host.hostname": "HOSTNAME.inera.se",
    "host.ip": "192.168.1.2",
    "host.os.family": "windows",
    "host.os.name": "Windows 11",
    "host.os.platform": "windows",
    "host.os.version": "10.0",
    "http.request.body.bytes": "582",
    "http.request.headers": "{\"CamelNettyRemoteAddress\":\"/127.0.0.1:51265\",\"content-length\":\"582\",\"CamelHttpUrl\":\"http://localhost:1028/vp/PATH1\",\"CamelHttpPort\":\"443\",\"CamelNettyLocalAddress\":\"/127.0.0.1:1028\",\"CamelHttpScheme\":\"null\",\"CamelHttpRawQuery\":\"null\",\"CamelNettyChannelHandlerContext\":\"ChannelHandlerContext(handler, [id: 0xda2487bd, L:/127.0.0.1:1028 - R:/127.0.0.1:51265])\",\"CamelNettySSLClientCertNotAfter\":\"Fri Feb 26 13:26:14 CET 2038\",\"CamelNettySSLClientCertSubjectName\":\"CN=client, OU=tp, O=SKL, L=Unknown, ST=GBG, C=SE\",\"CamelNettySSLClientCertSerialNumber\":\"17770512983148295944\",\"x-rivta-original-serviceconsumer-hsaid\":\"originalid\",\"CamelNettySSLClientCertNotBefore\":\"Mon Mar 04 13:26:14 CET 2013\",\"CamelHttpMethod\":\"POST\",\"x-vp-sender-id\":\"tp\",\"host\":\"localhost:1028\",\"CamelHttpQuery\":\"null\",\"connection\":\"keep-alive\",\"CamelHttpUri\":\"/vp/PATH1 \",\"CamelNettySSLSession\":\"Session(1771427368888|TLS_DHE_DSS_WITH_AES_128_CBC_SHA256)\",\"CamelHttpHost\":\"null\",\"CamelHttpPath\":\"/PATH1\",\"CamelNettySSLClientCertIssuerName\":\"CN=TheCA, OU=NOT FOR PRODUCTION, O=SKL, ST=GBG, C=SE\"}",
    "http.request.method": "POST",
    "labels.receiverid": "HttpProducer",
    "labels.rivversion": "rivtabp20",
    "labels.route": "vp-https-route",
    "labels.senderid": "tp",
    "labels.servicecontract_namespace": "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1",
    "labels.wsdl_namespace": "urn:riv:insuranceprocess:healthreporting:GetCertificate:1:rivtabp20",
    "message": "req-in tp -> HttpProducer",
    "service.name": "vp-services-test",
    "source.address": "127.0.0.1",
    "source.ip": "127.0.0.1",
    "span.id": "1ae1a050-b0da-46b5-ba08-7c64975c8306",
    "trace.id": "bd411a8a-4d60-496c-9bf8-9dc551fd011d",
    "transaction.id": "591E98EBD490843-0000000000000004",
    "url.full": "http://localhost:1028/vp/PATH1",
    "ecs.version": "1.2.0",
    "process.thread.name": "Camel (camel-1) thread #12 - NettyConsumerExecutorGroup",
    "log.logger": "se.skl.tp.vp.logging.req.in"
}
```
