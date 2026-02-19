# ECS Loggformat

## Översikt

Loggning i VP-tjänsten följer [Elastic Common Schema (ECS)](https://www.elastic.co/guide/en/ecs/current/index.html), vilket är ett öppet schema för strukturerad loggning. ECS definierar en gemensam uppsättning fält som möjliggör enhetlig loggning och analys över olika system.

## ECS Fältstruktur

### Basfält

Grundläggande fält som finns i alla loggposter:

| Fält          | ECS-referens                                                                                 | Beskrivning                                                     |
|---------------|----------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| `@timestamp`  | [@timestamp](https://www.elastic.co/guide/en/ecs/current/ecs-base.html#field-timestamp)      | Tidsstämpel för när händelsen inträffade (ISO 8601 format, UTC) |
| `message`     | [message](https://www.elastic.co/guide/en/ecs/current/ecs-base.html#field-message)           | Loggmeddelande som beskriver händelsen                          |

### Event-fält

Event-fält beskriver händelsen som loggas.

| Fält             | ECS-referens                                                                                      | Beskrivning                                                                                               |
|------------------|---------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `event.id`       | [event.id](https://www.elastic.co/guide/en/ecs/current/ecs-event.html#field-event-id)             | Unik identifierare för denna händelse (UUID)                                                              |
| `event.action`   | [event.action](https://www.elastic.co/guide/en/ecs/current/ecs-event.html#field-event-action)     | Den åtgärd som händelsen fångade (t.ex. `req-in`, `resp-out`)                                             |
| `event.module`   | [event.module](https://www.elastic.co/guide/en/ecs/current/ecs-event.html#field-event-module)     | Namnet på modulen som data kommer från (`skltp-messages` för HTTP/SOAP-meddelanden, `skltp-tls` för TLS/SSL-händelser) |
| `event.duration` | [event.duration](https://www.elastic.co/guide/en/ecs/current/ecs-event.html#field-event-duration) | Tidsåtgång för operationen i nanosekunder.                                                                |
| `event.kind`     | [event.kind](https://www.elastic.co/guide/en/ecs/current/ecs-event.html#field-event-kind)         | Första nivån i ECS kategorihierarki (alltid `event`)                                                      |
| `event.category` | [event.category](https://www.elastic.co/guide/en/ecs/current/ecs-event.html#field-event-category) | Andra nivån i ECS kategorihierarki (`["web"]` för HTTP/SOAP, `["configuration", "network"]` för TLS)     |
| `event.type`     | [event.type](https://www.elastic.co/guide/en/ecs/current/ecs-event.html#field-event-type)         | Tredje nivån i ECS kategorihierarki (`["access", "start"]` för request, `["access", "end"]` för response) |

#### Event Actions

VP-tjänsten använder olika `event.action`-värden beroende på händelsetyp:

**HTTP/SOAP-meddelanden** (`event.module`: `skltp-messages`):
- `req-in` - Inkommande request till VP
- `req-out` - Utgående request från VP till producent
- `resp-in` - Inkommande response från producent till VP
- `resp-out` - Utgående response från VP till konsument

**TLS/SSL-händelser** (`event.module`: `skltp-tls`):
- `ssl-context-register` - Registrering av en SSL-kontext med protokoll och cipher suites
- `tls-handshake-complete` - Slutförd TLS-handskakning med peer

### Tracing-fält

Tracing-fält används för distribuerad spårning och möjliggör korrelation av relaterade händelser över olika tjänster.

| Fält               | ECS-referens                                                                                 | Beskrivning                                                        |
|--------------------|----------------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| `trace.id`         | [trace.id](https://www.elastic.co/guide/en/ecs/current/ecs-tracing.html#field-trace-id)      | Korrelations-ID för spårning som kan spänna över flera system      |
| `span.id`          | [span.id](https://www.elastic.co/guide/en/ecs/current/ecs-tracing.html#field-span-id)        | Identifierar en specifik operation / flöde                         |
| `labels.parent.id` | -                                                                                            | Identifierar huvudoperationen i ett delflöde                       |
| `transaction.id`   | [transaction.id](https://www.elastic.co/docs/reference/ecs/ecs-tracing#field-transaction-id) | Identifierar den Camel exchange som används under hela operationen |

- För hela flödet från `req-in` till `resp-out` används ett `span.id`.
- För delflödet från `req-out` till `resp-in` används ett separat `span.id`. `labels.parent.id` sätts då till huvudflödet.

```
     Tjänstekonsument                          VP                          Tjänsteproducent
           │                                   │                                 │
           │                                   │                                 │   ┌─────────────────
           │──────────────────────── req-in ──>│                                 │   │
           │                    event.id: 1    │                                 │   │ trace.id:  X
           │                                   │                                 │   │ parent.id: -
           │                                   │                                 │   │ span.id:   A
           │                                   │                                 │   │   ┌──────────────
           │                                   │─── req-out ────────────────────>│   │   │
           │                                   │    event.id: 2                  │   │   │ trace.id:  X
           │                                   │                                 │   │   │ parent.id: A
           │                                   │                                 │   │   │ span.id:   B
           │                                   │<── resp-in ─────────────────────│   │   │
           │                                   │    event.id: 3                  │   │   │
           │                                   │                                 │   │   └──────────────
           │<───────────────────── resp-out ───│                                 │   │
           │                    event.id: 4    │                                 │   │
           │                                   │                                 │   └──────────────────

```

- `event.duration` mäter tiden från `req-in` till `resp-out` för huvudflödet. (tid för hela anropet)
- `event.duration` mäter tiden från `req-out` till `resp-in` för delflödet. (tid för anrop mot tjänsteproducent)

### HTTP-fält

#### Request-fält

| Fält                        | ECS-referens                                                                                                           | Beskrivning                                                       |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|
| `http.request.method`       | [http.request.method](https://www.elastic.co/guide/en/ecs/current/ecs-http.html#field-http-request-method)             | HTTP-metod (t.ex. GET, POST)                                      |
| `http.request.headers`      | [http.request.headers](https://doc.wikimedia.org/ecs/#field-http-request-headers)                                      | HTTP request headers som JSON-objekt (känsliga headers filtreras) |
| `http.request.body.bytes`   | [http.request.body.bytes](https://www.elastic.co/guide/en/ecs/current/ecs-http.html#field-http-request-body-bytes)     | Storlek på request body i bytes                                   |
| `http.request.body.content` | [http.request.body.content](https://www.elastic.co/guide/en/ecs/current/ecs-http.html#field-http-request-body-content) | Request body-innehåll (används ej i produktion)                   |

#### Response-fält

| Fält                         | ECS-referens                                                                                                             | Beskrivning                                      |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| `http.response.status_code`  | [http.response.status_code](https://www.elastic.co/guide/en/ecs/current/ecs-http.html#field-http-response-status-code)   | HTTP-statuskod                                   |
| `http.response.headers`      | [http.response.headers](https://doc.wikimedia.org/ecs/#field-http-response-headers)                                      | HTTP response headers som JSON-objekt            |
| `http.response.body.bytes`   | [http.response.body.bytes](https://www.elastic.co/guide/en/ecs/current/ecs-http.html#field-http-response-body-bytes)     | Storlek på response body i bytes                 |
| `http.response.body.content` | [http.response.body.content](https://www.elastic.co/guide/en/ecs/current/ecs-http.html#field-http-response-body-content) | Response body-innehåll (används ej i produktion) |

### URL-fält

URL-fält beskriver de URL:er som är involverade i kommunikationen.

| Fält           | ECS-referens                                                                                | Beskrivning                                                   |
|----------------|---------------------------------------------------------------------------------------------|---------------------------------------------------------------|
| `url.full`     | [url.full](https://www.elastic.co/guide/en/ecs/current/ecs-url.html#field-url-full)         | Komplett URL för den inkommande requesten till tjänsten       |
| `url.original` | [url.original](https://www.elastic.co/guide/en/ecs/current/ecs-url.html#field-url-original) | Komplett URL för den utgående requesten till backend (vägval) |

### Source och Destination-fält

Dessa fält beskriver källan och målet för nätverkstrafiken.

#### Source (Källa)

| Fält             | ECS-referens                                                                                       | Beskrivning    |
|------------------|----------------------------------------------------------------------------------------------------|----------------|
| `source.address` | [source.address](https://www.elastic.co/guide/en/ecs/current/ecs-source.html#field-source-address) | Källadress     |
| `source.ip`      | [source.ip](https://www.elastic.co/guide/en/ecs/current/ecs-source.html#field-source-ip)           | Käll-IP-adress |

#### Destination (Destination)

| Fält                  | ECS-referens                                                                                                      | Beskrivning               |
|-----------------------|-------------------------------------------------------------------------------------------------------------------|---------------------------|
| `destination.address` | [destination.address](https://www.elastic.co/guide/en/ecs/current/ecs-destination.html#field-destination-address) | Destinationsadress        |
| `destination.domain`  | [destination.domain](https://www.elastic.co/guide/en/ecs/current/ecs-destination.html#field-destination-domain)   | Destinationens domännamn  |
| `destination.port`    | [destination.port](https://www.elastic.co/guide/en/ecs/current/ecs-destination.html#field-destination-port)       | Destinationens portnummer |

### Error-fält

Error-fält används när ett fel uppstår under bearbetningen.

| Fält                | ECS-referens                                                                                            | Beskrivning                       |
|---------------------|---------------------------------------------------------------------------------------------------------|-----------------------------------|
| `error.type`        | [error.type](https://www.elastic.co/guide/en/ecs/current/ecs-error.html#field-error-type)               | Feltyp, t.ex. exception-klassnamn |
| `error.message`     | [error.message](https://www.elastic.co/guide/en/ecs/current/ecs-error.html#field-error-message)         | Felmeddelande som beskriver felet |
| `error.stack_trace` | [error.stack_trace](https://www.elastic.co/guide/en/ecs/current/ecs-error.html#field-error-stack-trace) | Stack trace för felet i klartext  |

### Service-fält

Service-fält beskriver tjänsten som genererar loggposten.

| Fält           | ECS-referens                                                                                    | Beskrivning    |
|----------------|-------------------------------------------------------------------------------------------------|----------------|
| `service.name` | [service.name](https://www.elastic.co/guide/en/ecs/current/ecs-service.html#field-service-name) | Tjänstens namn |

### Host-fält

Host-fält beskriver den värd (server/container) där VP-tjänsten körs.

| Fält                 | ECS-referens                                                                                                  | Beskrivning                                                    |
|----------------------|---------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| `host.hostname`      | [host.hostname](https://www.elastic.co/guide/en/ecs/current/ecs-host.html#field-host-hostname)                | Värdmaskinens namn                                             |
| `host.ip`            | [host.ip](https://www.elastic.co/guide/en/ecs/current/ecs-host.html#field-host-ip)                            | IP-adress för värdmaskinen                                     |
| `host.architecture`  | [host.architecture](https://www.elastic.co/guide/en/ecs/current/ecs-host.html#field-host-architecture)        | Värdmaskinens arkitektur (t.ex. x86_64, arm64)                 |
| `host.os.family`     | [host.os.family](https://www.elastic.co/guide/en/ecs/current/ecs-host.html#field-host-os-family)              | Operativsystemets familj (t.ex. windows, unix, darwin)         |
| `host.os.name`       | [host.os.name](https://www.elastic.co/guide/en/ecs/current/ecs-host.html#field-host-os-name)                  | Operativsystemets namn (t.ex. Linux, Windows Server 2019)      |
| `host.os.version`    | [host.os.version](https://www.elastic.co/guide/en/ecs/current/ecs-host.html#field-host-os-version)            | Operativsystemets version                                      |
| `host.os.platform`   | [host.os.platform](https://www.elastic.co/guide/en/ecs/current/ecs-host.html#field-host-os-platform)          | Operativsystemets plattform (t.ex. linux, windows, darwin)     |
| `host.type`          | [host.type](https://www.elastic.co/guide/en/ecs/current/ecs-host.html#field-host-type)                        | Typ av värd (t.ex. container, vm, physical)                    |

### Log-fält

Detaljer om loggmekanismen.

| Fält          | ECS-referens                                                                                 | Beskrivning                                                     |
|---------------|----------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| `log.level`   | [log.level](https://www.elastic.co/guide/en/ecs/current/ecs-log.html#field-log-level)        | Loggnivå för händelsen (t.ex. `DEBUG`, `INFO`, `ERROR`)         |
| `log.logger`  | [log.logger](https://www.elastic.co/guide/en/ecs/current/ecs-log.html#field-log-logger)      | Namnet på loggern som genererade händelsen                      |

### TLS/SSL-fält

TLS/SSL-fält beskriver säkerhetsaspekter av krypterade anslutningar.

| Fält                   | ECS-referens                                                                                                     | Beskrivning                                                        |
|------------------------|------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| `tls.version_protocol` | [tls.version_protocol](https://www.elastic.co/guide/en/ecs/current/ecs-tls.html#field-tls-version-protocol)      | Normaliserat protokollnamn i gemener (t.ex. `tls`, `ssl`)          |
| `tls.version`          | [tls.version](https://www.elastic.co/guide/en/ecs/current/ecs-tls.html#field-tls-version)                        | Numerisk version från protokollsträngen (t.ex. `1.2`, `1.3`)       |
| `tls.cipher`           | [tls.cipher](https://www.elastic.co/guide/en/ecs/current/ecs-tls.html#field-tls-cipher)                          | Cipher suite som används i anslutningen                            |
| `tls.established`      | [tls.established](https://www.elastic.co/guide/en/ecs/current/ecs-tls.html#field-tls-established)                | Om TLS-anslutningen etablerades framgångsrikt (boolean som sträng) |

#### Server-fält (för TLS handshake-loggning)

| Fält             | ECS-referens                                                                                       | Beskrivning                   |
|------------------|----------------------------------------------------------------------------------------------------|-------------------------------|
| `server.address` | [server.address](https://www.elastic.co/guide/en/ecs/current/ecs-server.html#field-server-address) | Serveradress (värdnamn eller IP) |


## Labels (Anpassade fält)

Labels används för att lägga till domänspecifik metadata som inte täcks av standard ECS-fält.

### HTTP X-Forwarded Headers

| Label                        | Beskrivning                             |
|------------------------------|-----------------------------------------|
| `labels.httpXForwardedProto` | Protokoll från X-Forwarded-Proto header |
| `labels.httpXForwardedHost`  | Host från X-Forwarded-Host header       |
| `labels.httpXForwardedPort`  | Port från X-Forwarded-Port header       |

### Felhantering

| Label                                     | Beskrivning                                          |
|-------------------------------------------|------------------------------------------------------|
| `labels.sessionStatus`                    | Sätts till `"true"` vid fel under processningen i VP |
| `labels.sessionErrorDescription`          | Beskrivning av felet                                 |
| `labels.sessionErrorTechnicalDescription` | Teknisk beskrivning av felet (exception.toString())  |
| `labels.errorCode`                        | VP-specifik felkod                                   |
| `labels.statusCode`                       | HTTP-statuskod och beskrivning                       |
| `labels.faultCode`                        | SOAP fault code från tjänsteproducenten              |
| `labels.faultString`                      | SOAP fault string från tjänsteproducenten            |
| `labels.faultDetail`                      | SOAP fault detail från tjänsteproducenten            |

### HSA-identifierare

| Label                                    | Beskrivning                                                                                |
|------------------------------------------|--------------------------------------------------------------------------------------------|
| `labels.senderid`                        | HSA-ID för avsändaren av meddelandet                                                       |
| `labels.receiverid`                      | HSA-ID för mottagaren av meddelandet                                                       |
| `labels.originalServiceconsumerHsaid`    | Ursprungligt HSA-ID (utgående trafik)                                                      |
| `labels.originalServiceconsumerHsaid_in` | Ursprungligt HSA-ID (inkommande trafik)                                                    |
| `labels.actingOnBehalfOfHsaid`           | HSA-ID för ursprunglig tjänstekomponent (från `x-rivta-acting-on-behalf-of-hsa-id` header) |

### RIV/WSDL-metadata

| Label                              | Beskrivning                                                                                                                                                                        |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `labels.servicecontract_namespace` | [Tjänsteinteraktionens target namespace](https://inera.atlassian.net/wiki/spaces/RTA/pages/3632903/RIV+Tekniska+Anvisningar+Tj+nsteschema#Regel-#3,-namn-p%C3%A5-target-namespace) |
| `labels.rivversion`                | RIV-version/profilkortnamn (t.ex. "RIVTABP21")                                                                                                                                     |
| `labels.wsdl_namespace`            | [WSDL target namespace](https://inera.atlassian.net/wiki/spaces/RTA/pages/3632875/RIV+Tekniska+Anvisningar+Basic+Profile+2.1#Regel-#4,-namn-p%C3%A5-target-namespace)              |

### Spårning

| Label                          | Beskrivning                                                             |
|--------------------------------|-------------------------------------------------------------------------|
| `labels.route`                 | Intern route-identifierare                                              |
| `labels.routerVagvalTrace`     | Spårningsinformation från vägval-routing                                |
| `labels.routerBehorighetTrace` | Spårningsinformation från anropsbehörighetskontroll                     |
| `labels.parent.id`             | Identifierar huvudoperationen i ett delflöde. Se även fältet `span.id`. |

### TLS/SSL-konfiguration

TLS/SSL-specifika labels används för loggning av SSL-kontextregistrering och handshake-händelser.

| Label                         | Beskrivning                                                           |
|-------------------------------|-----------------------------------------------------------------------|
| `labels.sslContextId`         | Identifierare för SSL-kontexten                                       |
| `labels.protocolCount`        | Antal tillgängliga TLS-protokoll i SSL-kontexten                      |
| `labels.protocols`            | Lista över tillgängliga TLS-protokoll (JSON-array)                    |
| `labels.cipherSuiteCount`     | Antal tillgängliga cipher suites i SSL-kontexten                      |
| `labels.cipherSuites`         | Lista över tillgängliga cipher suites (JSON-array)                    |
| `labels.clientCertCount`      | Antal klientcertifikat som skickades under TLS-handshake              |


## Referenser

- [Elastic Common Schema (ECS) Reference](https://www.elastic.co/guide/en/ecs/current/index.html)
- [ECS Field Reference](https://www.elastic.co/guide/en/ecs/current/ecs-field-reference.html)
- [ECS Usage Guidelines](https://www.elastic.co/guide/en/ecs/current/ecs-using-ecs.html)
