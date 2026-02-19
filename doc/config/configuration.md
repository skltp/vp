# Konfiguration av VP Camel

Konfigurering kan göras i filerna listade nedan. Vid respektive avsnitt finns information om funktion och vad som kan ändras.
  
 * application.properties  
 * application-security.properties  
 * timeoutconfig.json  
 * wsdlconfig.json

För mer information om hur eventuell proxy eller lastbalanserare ska konfigureras, hur användare och lösenord för Hawtio konfigureras samt exempelfiler, se [Detaljerad konfiguration].
Loggning och hur det går till och kan konfigureras kan man läsa om här: [Loggning konfiguration]. Loggformatet beskrivs här: [Loggformat].

### Application.properties ###
Spring-boot property fil som ligger under resources i jaren. Inställningarna kan överlagras enligt de sätt som Spring-boot föreskriver. 

|Nyckel|Defaultvärde/Exempel| Beskrivning                                                                                                                                                                                                                                                                                                                                                   |
|----|------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| server.port | 8880 | Porten som servern ska starta på                                                                                                                                                                                                                                                                                                                              |
|server.use.forward.headers|false| Om VP Camel befinner sig bakom en proxy, sätt denna till true. Se vidare här: VP Camel Detaljerad konfiguration. Om propertyn saknas är defaultvärdet ```false```.                                                                                                                                                                                            |
|server.undertow.accesslog.dir|var/log/camel| Konfiguration för undertow accesslog: Var filerna ska lagras. Se till exempel [Tips på hur man konfigurerar undertow] eller sök på undertow på sidan [Spring-boot doc's]                                                                                                                                                                                      |
|server.undertow.accesslog.enabled|true| Konfiguration för undertow accesslog: På/av                                                                                                                                                                                                                                                                                                                   |
|server.undertow.accesslog.pattern|common| Konfiguration för undertow accesslog: Format på loggningen                                                                                                                                                                                                                                                                                                    |
|server.undertow.accesslog.prefix|access_log| Konfiguration för undertow accesslog: Prefix på logg-filen                                                                                                                                                                                                                                                                                                    |
|server.undertow.accesslog.rotate|true| Konfiguration för undertow accesslog: Använda rotering av filer                                                                                                                                                                                                                                                                                               |
|server.undertow.accesslog.suffix|.log| Konfiguration för undertow accesslog: Suffix på logg-filen                                                                                                                                                                                                                                                                                                    |
|spring.profiles.include|security| Sätt vilka Spring profiler som skall användas                                                                                                                                                                                                                                                                                                                 |
|camel.springboot.name|vp-services| Namn på Spring-boot applikationen                                                                                                                                                                                                                                                                                                                             |
| management.endpoints.web.exposure.include | hawtio,jolokia,health | Default exponera Hawtio och health-indicators                                                                                                                                                                                                                                                                                                                 |
| management.endpoint.health.probes.enabled | true | Exponera liveness/readiness probes                                                                                                                                                                                                                                                                                                                            |
| management.endpoint.health.show-details | always | Visa detaljer om health indicators                                                                                                                                                                                                                                                                                                                            |
| management.health.livenessState.enabled | true | Aktivera inbyggd livess-indikator                                                                                                                                                                                                                                                                                                                             |
| management.health.readinessState.enabled | true | Aktivera inbyggd readiness-indikator                                                                                                                                                                                                                                                                                                                          |
| management.endpoint.health.group.liveness.include | livenessState | Anger vilka indikatorer som ingår i liveness-proben                                                                                                                                                                                                                                                                                                           |
| management.endpoint.health.group.readiness.include | readinessState,takCache,hsaCache | Anger vilka indikatorer som ingår i readiness-proben. Default ingår kontroll att TAK Cache och HSA Cache är initierade                                                                                                                                                                                                                                        |
|hawtio.authentication.enabled|false| Aktivera/Avaktivera autentisering för att använda Hawtio. Defaultvärde false.                                                                                                                                                                                                                                                                                 |
|hawtio.external.loginfile|\<path>/realm-custom.properties| Sökväg till extern login-fil. Se [Detaljerad konfiguration]                                                                                                                                                                                                                                                                   
|vp.logging.style|LEGACY| Anger vilket loggformat som ska användas. Möjliga värden: `ECS` (Elastic Common Schema, rekommenderat) eller `LEGACY` (äldre format, deprecated). Om parametern saknas används `LEGACY` som default. Se [Loggformat] och [Migreringsguide loggning] för mer information.                                                     
|vp.instance.id|dev_env| Identifierare för den installerade VP:n                                                                                                                                                                                                                                                                                                                       |
|vp.instance.name|SKLTP_DEFAULT_NAME| Namn för den installerade VP:n, används i felmeddelanden. Ändra till ett unikt namn                                                                                                                                                                                                                                                                           |
|vp.http.route.url|htttp://localhost:12312/vp| Ingång för HTTP-anrop. Porten kan konfigureras                                                                                                                                                                                                                                                                                                                |
|vp.https.route.url|https://localhost:443/vp| Ingång för HTTPS-anrop. Porten kan konfigureras                                                                                                                                                                                                                                                                                                               |
|vp.hsa.reset.cache.url|http://localhost:24000/resethsacache| Ingång för anrop för att uppdatera HSA-cachen. Porten kan konfigureras                                                                                                                                                                                                                                                                                        |
|vp.reset.cache.url|http://localhost:24000/resetcache| Ingång för anrop för att uppdatera TAK-cachen. Porten kan konfigureras                                                                                                                                                                                                                                                                                        |
|vp.status.url|http://localhost:1080/status| Adressen till status-tjänsten, se även [SKLTP VP - Status tjänst]                                                                                                                                                                                                                                                                                             |
|management.security.enabled|false| False: Tillåt access till alla endpoints utan säkerhets-kontroll                                                                                                                                                                                                                                                                                              |
|endpoints.health.enabled|true| True: Slå på health-check för endpoints                                                                                                                                                                                                                                                                                                                       |
|endpoints.camelroutes.enabled|true| Medger tillgång till information om de Camel-routes som finns                                                                                                                                                                                                                                                                                                 |
|endpoints.camelroutes.read-only|true| Tillgång till endpoints bara i read-only mode                                                                                                                                                                                                                                                                                                                 |
|certificate.senderid.subject.pattern|(?:2.5.4.5\| SERIALNUMBER)=([^,]+)                                                                                                                                                                                                                                                                                                                                         |Var i certifikatet hittas senderId (reg-exp pattern)|
|http.forwarded.header.xfor|X-Forwarded-For| Reverse proxy/LB header-avsändare                                                                                                                                                                                                                                                                                                                             |
|http.forwarded.header.host|X-VP-Forwarded-Host| Reverse proxy/LB header-Intern host                                                                                                                                                                                                                                                                                                                           |
|http.forwarded.header.port|X-VP-Forwarded-Port| Reverse proxy/LB header-Intern port                                                                                                                                                                                                                                                                                                                           |
|http.forwarded.header.proto|X-VP-Forwarded-Proto| Reverse proxy/LB header-Protokoll                                                                                                                                                                                                                                                                                                                             |
|http.forwarded.header.auth_cert|x-vp-auth-cert| Reverse proxy/LB header-Certifikat från inkommande https-anrop från konsument                                                                                                                                                                                                                                                                                 |
|ip.whitelist|127.0.0.1| Komma-separerad lista. Vilka IP-adresser får sätta headern ```sender-id``` för http-anrop. Listan kan vara tom, vilket gör att alla tillåts                                                                                                                                                                                                                   |
|sender.id.allowed.list| | Komma-separerad lista av vilka avsändare(HSA-IDn) som får sätta headern ```x-rivta-original-serviceconsumer-hsaid ```. Tom lista innebär att alla avsändare är tillåtna. Är parametern ```throw.vp013.when.originalconsumer.not.allowed``` satt till ```true``` kommer VP013 returneras annars skrivs en varning i loggen och anropet fortsätter som vanligt. |
|throw.vp013.when.originalconsumer.not.allowed|false| Används i kombination med ```sender.id.allowed.list```. ```true``` betyder att VP013 kastas om en ej godkänd avsändare försöker sätta headern. Vid ```false``` kommer en varning skrivas till loggen att avsändaren inte är godkänd men transaktionen forsätter som vanligt.                                                                                  |
|propagate.correlation.id.for.https|false| Ska korrelations-id:t propageras vidare även för https?                                                                                                                                                                                                                                                                                                       |
|vp.header.user.agent|SKLTP VP/3.1| User_Agent för utgående requests                                                                                                                                                                                                                                                                                                                              |
|vp.header.content.type|text/xml;charset=UTF-8| Content-type för utgående requests                                                                                                                                                                                                                                                                                                                            |
|hsa.files|<file_path>/hsacache.xml,<file_path>/hsacacheComplementary.xml| Lista med filer som ska läsas av HSA cachen. Den första master, övriga kompletterande                                                                                                                                                                                                                                                                         |
|hsa.lookup.behorighet.defaultEnabled | true | Anger om hierarkisk behörighetskontroll (trädklättring för anropsbehörighter) är aktiverat i de fall inte undantag angivits. |
|hsa.lookup.behorighet.exceptedNamespaces | | Undantag från ovanstående default-inställning för behörigheter. Anges som komma-separerad lista med tjänstekontrakt/domäner. Exempel: "urn:riv:clinicalprocess:activity:actions:GetActivitiesResponder:1,urn:riv:crm:scheduling". Matching sker på början av namnrymden. |
|hsa.lookup.vagval.defaultEnabled | true | Anger om hierarkisk routing (trädklättring för vägval) är aktiverat i de fall inte undantag angivits. |
|hsa.lookup.vagval.exceptedNamespaces | | Undantag från ovanstående default-inställning för vägval. Samma syntax och logik som motsvarande parameter för behörigheter. |
|vagvalrouter.default.routing.address.delimiter|#| Avgör om default routing ska användas (VG#VE) när man evaluerar vägval och behörigheter. Om värdet inte är satt så är default routing avstängt                                                                                                                                                                                                                |
|takcache.persistent.file.name|c:/tmp/vp-camel/local-tak-cache.xml| Sökväg och namn till TAK-cachen                                                                                                                                                                                                                                                                                                                               |
|takcache.endpoint.address|http://localhost:8882/takmockservice| Var ska cachen förnyas, dvs var finns den installerade TAK:en?                                                                                                                                                                                                                                                                                                |
|timeout.json.file|timeoutconfig.json| Sökväg till default eller skapad timeoutconfig-fil                                                                                                                                                                                                                                                                                                            |
|timeout.json.file.default.tjansteKontrakt.name|default_timeouts| Vilket tjänstekontrakts timeout-värden ska anändas som default?                                                                                                                                                                                                                                                                                               |
|wsdl.json.file|wsdlconfig.json| Namn på json-fil med lista av wsdl:er som har url:er som inte följer standard. Se avsnitt om Wsdlconfig.json nedan                                                                                                                                                                                                                                            |
|wsdlfiles.directory|C:/vp/wsdl/| Sökväg till mapp med wdsl-filer. De installerade kontrakten                                                                                                                                                                                                                                                                                                   |
|headers.reg.exp.requestHeadersToRemove|(?i)x-vp.*\| PEER_CERTIFICATES\                                                                                                                                                                                                                                                                                                                                            |X-Forwarded.*\|MULE_.*\|X-MULE_.*\|Connection\|accept-encoding|Filtrerar bort oönskade headrar från utgående request|
|headers.reg.exp.requestHeadersToKeep|(?i)x-vp-sender-id\| x-vp-instance-id                                                                                                                                                                                                                                                                                                                                              |Headrar som ska behållas i utgående request|
|headers.reg.exp.responseHeadersToRemove|(?i)SOAPAction\| MULE_.*\                                                                                                                                                                                                                                                                                                                                                      |X-MULE_.*\|LOCAL_CERTIFICATE\|PEER_CERTIFICATES\|http.method|Filtrerar bort oönskade headrar från response|
|headers.reg.exp.responseHeadersToKeep||Headrar som ska behållas i response|
|vp.producer.retry.attempts|1| Hur många gånger ska producenten anropas vid misslyckat anrop? Negativt värde gör att den aldrig ger upp, 0 att den bara gör det första försöket                                                                                                                                                                                                              |
|vp.producer.retry.delay|2000| Hur länge ska vp vänta till nästa försök, vid misslyckat anrop till producent (mS)                                                                                                                                                                                                                                                                            |
|vp.maxreceive.length|157286640| Maxstorlek i bytes för Response, 15 mB                                                                                                                                                                                                                                                                                                                        |
|producer.http.connect.timeout|2000| Connect timeout mot http producent (mS)                                                                                                                                                                                                                                                                                                                       |
|producer.https.connect.timeout|2000| Connect timeout mot https producent (mS)                                                                                                                                                                                                                                                                                                                      |
|producer.http.disconnect=false|false| Koppla ner efter anrop mot producent                                                                                                                                                                                                                                                                                                                          |
|producer.https.disconnect=false|false| Koppla ner efter anrop mot producent                                                                                                                                                                                                                                                                                                                          |
|producer.http.keepAlive|true| Sätt keepAlive http header mot http producent samt aktivera keepAlive på socket                                                                                                                                                                                                                                                                               |
|producer.https.keepAlive|true| Sätt keepAlive http header mot https producent samt aktivera keepAlive på socket                                                                                                                                                                                                                                                                              |
|producer.http.workers|50| Antal Netty Eventloop threads för http producer                                                                                                                                                                                                                                                                                                               |
|producer.https.workers|150| Antal Netty Eventloop threads för https producer                                                                                                                                                                                                                                                                                                              |
|producer.https.hostnameVerification|false| Kontroll av hostname gentemot certifikat för https producenter. Default avstängt för bakåtkompatibilitet.                                                                                                                                                                                                                                                     |
|vp.use.routing.history|true| Anger ifall rundgångsskydd ska användas                                                                                                                                                                                                                                                                                                                       |
|message.logger.method|classic| Kan sättas till "object" för att logga meddelanden i ett sk ObjectMessage istället för en sträng ("classic")                                                                                                                                                                                                                                                  |
### Application-security.properties ###
Default Spring-boot property fil (Kräver att Spring profilen 'security' är aktiverad). Denna fil i original ligger under resources i jaren. Inställningarna kan överlagras enligt de sätt som Spring-boot föreskriver. 

#### SSL Bundles ####
SSL bundles är det rekommenderade sättet att konfigurera certifikat i moderna Spring Boot-applikationer. Bundles definieras med `spring.ssl.bundle.*` och refereras sedan från `vp.tls.*` konfigurationen.

|Nyckel|Defaultvärde/Exempel|Beskrivning|
|------|--------------------|----------|
|spring.ssl.bundle.pem.\<bundle-name\>.keystore.certificate|classpath:/certs/client-cert.pem|Sökväg till certifikatfilen (PEM-format)|
|spring.ssl.bundle.pem.\<bundle-name\>.keystore.private-key|classpath:/certs/client-key.pem|Sökväg till privat nyckel (PEM-format)|
|spring.ssl.bundle.pem.\<bundle-name\>.keystore.private-key-password|<password>|Lösenord för privat nyckel (valfritt)|
|spring.ssl.bundle.pem.\<bundle-name\>.truststore.certificate|classpath:/certs/ca-cert.pem|Sökväg till CA-certifikat för trust (PEM-format)|

Exempel på bundle-definition:
```properties
spring.ssl.bundle.pem.prod.keystore.certificate=classpath:/certs/tp-cert.pem
spring.ssl.bundle.pem.prod.keystore.private-key=classpath:/certs/tp-key.pem
spring.ssl.bundle.pem.prod.keystore.private-key-password=password
spring.ssl.bundle.pem.prod.truststore.certificate=classpath:/certs/ca-cert.pem

spring.ssl.bundle.pem.cons.truststore.certificate=classpath:/certs/ca-cert.pem
```

#### TLS-konfiguration (vp.tls.*) ####
**OBS: Detta är den rekommenderade konfigurationen. Den ersätter den äldre `tp.tls.*` konfigurationen för utgående trafik.**

TLS-konfigurationen styr vilka SSL-bundles som ska användas samt vilka protokoll och cipher suites som är tillåtna. Konfigurationen stödjer både en default-konfiguration och specifika overrides för olika mål baserat på domännamn, suffix eller port.

##### Default-konfiguration #####
|Nyckel|Defaultvärde/Exempel| Beskrivning                                                                                |
|------|--------------------|--------------------------------------------------------------------------------------------|
|vp.tls.default-config.name|default| Namn på konfigurationen. Måste vara unikt.                                                 |
|vp.tls.default-config.bundle|prod| Namn på SSL-bundle som ska användas (refererar till spring.ssl.bundle.pem.\<bundle-name\>) |
|vp.tls.default-config.protocols-include|TLSv1.2,TLSv1.3| Komma-separerad lista av tillåtna TLS-protokoll (vitlistning)                              |
|vp.tls.default-config.protocols-exclude| | Komma-separerad lista av protokoll som ska exkluderas (svartlistning)                      |
|vp.tls.default-config.cipher-suites-include|TLS_AES_256_GCM_SHA384,...| Komma-separerad lista av tillåtna cipher suites (vitlistning)                              |
|vp.tls.default-config.cipher-suites-exclude| | Komma-separerad lista av cipher suites som ska exkluderas (svartlistning)                  |

Namn och bundle är obligatoriska. Protokoll och cipher suites är valfria. Om ingen specificeras används Spring Boots default-värden.

Använd antingen vitlistning eller svartlistning, inte båda. Om include används tas endast de angivna med. Om exclude används tas alla default-värden utom de angivna med.

##### Override-konfiguration #####
Overrides används för att konfigurera specifika TLS-inställningar för vissa mål. Detta är användbart när olika tjänsteproducenter kräver olika säkerhetsinställningar.

|Nyckel|Defaultvärde/Exempel| Beskrivning                                                               |
|------|--------------------|---------------------------------------------------------------------------|
|vp.tls.overrides[].name|strict| Namn på konfigurationen. Måste vara unikt.                                |
|vp.tls.overrides[].bundle|strict-bundle| Namn på SSL-bundle för denna override                                     |
|vp.tls.overrides[].protocols-include|TLSv1.3| Tillåtna protokoll för denna override (vitlistning)                       |
|vp.tls.default-config.protocols-exclude| | Komma-separerad lista av protokoll som ska exkluderas (svartlistning)     |
|vp.tls.overrides[].cipher-suites-include|TLS_AES_256_GCM_SHA384| Tillåtna cipher suites för denna override (vitlistning)                   |
|vp.tls.default-config.cipher-suites-exclude| | Komma-separerad lista av cipher suites som ska exkluderas (svartlistning) |
|vp.tls.overrides[].match.domain-name|secure.example.com| Matcha exakt domännamn                                                    |
|vp.tls.overrides[].match.domain-suffix|.example.com| Matcha domänsuffix                                                        |
|vp.tls.overrides[].match.port|8443| Matcha specifik port                                                      |

Namn, bundle och någon match-konfiguration är obligatoriska. Protokoll och cipher suites är valfria. Om ingen specificeras används Spring Boots default-värden.

Använd antingen vitlistning eller svartlistning, inte båda. Om include används tas endast de angivna med. Om exclude används tas alla default-värden utom de angivna med.

Exakt matchning av domännamn prioriteras högst, så om både `domain-name` och `domain-suffix` är angivna, kommer `domain-name` att användas först.
Portmatchning kan kombineras med domänmatchning för mer specifika regler.

Exempel på override-konfiguration:
```properties
# Override för strängare säkerhet mot vissa domäner
vp.tls.overrides[0].name=strict
vp.tls.overrides[0].bundle=strict-bundle
vp.tls.overrides[0].protocols-include=TLSv1.3
vp.tls.overrides[0].cipher-suites-include=TLS_AES_256_GCM_SHA384
vp.tls.overrides[0].match.domain-suffix=.secure-domain.com

# Override för legacy-system som kräver äldre protokoll
vp.tls.overrides[1].name=legacy
vp.tls.overrides[1].bundle=legacy-bundle
vp.tls.overrides[1].protocols-include=TLSv1.2,TLSv1.1
vp.tls.overrides[1].match.domain-name=legacy.example.com
vp.tls.overrides[1].match.port=8080
```

Övriga konfigurationsparametrar:

|Nyckel| Defaultvärde/Exempel | Beskrivning                                                                                                                         |
|------|----------------------|-------------------------------------------------------------------------------------------------------------------------------------|
|vp.tls.mtls-verification-enabled| false                | Logga ett fel om mTLS inte användes vid TLS-förhandlingen. Loggningen sker i se.skl.tp.vp.sslcontext.MtlsAwareSSLContextParameters. |

#### tp.tls.* (DEPRECATED) ####
**️ VARNING: Denna konfiguration är deprecated och kommer att tas bort i framtida versioner. Migrera till `vp.tls.*` och SSL bundles så snart som möjligt.**

|Nyckel|Defaultvärde/Exempel|Beskrivning|
|------|--------------------|----------|
|tp.tls.store.location|/certs/|**(DEPRECATED)** Mapp där certifikaten kan hittas|
|tp.tls.store.truststore.file|truststore.jks|**(DEPRECATED)** Ska innehålla namn på de CA's och Certifikat som VP kan lita på|
|tp.tls.store.truststore.password|<password>|**(DEPRECATED)** Lösenord för trust-store|
|tp.tls.store.producer.file|tp.jks|**(DEPRECATED)** Certifikat som används av VP i rollen som producent|
|tp.tls.store.producer.password|<password>|**(DEPRECATED)** Lösenord för producent-certifikatet|
|tp.tls.store.producer.keyPassword|<password>|**(DEPRECATED)** Lösenord för den privata nyckeln i producent-certifikatet|
|tp.tls.store.consumer.file|client.jks|**(DEPRECATED)** Certifikat som används av VP i rollen som konsument|
|tp.tls.store.consumer.password|<password>|**(DEPRECATED)** Lösenord för konsument-certifikatet|
|tp.tls.store.consumer.keyPassword|<password>|**(DEPRECATED)** Lösenord för den privata nyckeln i konsument-certifikatet|
|tp.tls.allowedIncomingProtocols|TLSv1,TLSv1.1,TLSv1.2|**(DEPRECATED)** Godkända protokoll för inkommande trafik|
|tp.tls.allowedOutgoingProtocols |TLSv1,TLSv1.1,TLSv1.2|**(DEPRECATED)** Godkända protokoll för utgående trafik|
|tp.tls.allowedIncomingCipherSuites|*|**(DEPRECATED)** Godkända cipher suites för inkommande trafik (* = alla)|
|tp.tls.allowedOutgoingCipherSuites|*|**(DEPRECATED)** Godkända cipher suites för utgående trafik (* = alla)|


[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)


   [Detaljerad konfiguration]: <detail_config.md>
   [Loggning konfiguration]: <logging_configuration.md>
   [Loggformat]: <ecs_log_format.md>
   [Migreringsguide loggning]: <logging_migration_guide.md>
   [Tips på hur man konfigurerar undertow]: <https://howtodoinjava.com/spring-boot2/embedded-server-logging-config/>
   [Spring-boot doc's]: <https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html>
   [SKLTP VP - Status tjänst]: <https://inera.atlassian.net/wiki/spaces/SKLTP/pages/3187836663/SKLTP+VP+-+Status+tj+nst>
