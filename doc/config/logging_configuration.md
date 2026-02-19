# Logging konfigurering

Denna sida handlar om loggning. För allmän information om konfiguration av VP Camel, se [VP Camel konfigurering].
### Allmänt
Som logg-ramverk används Log4j2, se [log4j2s dokumentation] för mer information.
En grundkonfigurering finns i projektet under `resources/log4j2.xml` som default loggar till konsolen.
Det är möjligt att konfigurera json-formaterade loggar med hjälp av log4j2-ecs-layout, se [ecs-logging dokumentation] för mer information.

### Extern konfiguration
För att använda en extern log4j2.xml konfigurationsfil kan man starta applikationen med parametern `-Dlog4j.configurationFile`, 
till exempel:
```shell
java -jar -Xms256m -Xmx1024m -Dlog4j.configurationFile=file:///opt/vp/config/log4j2.xml vp-services.jar
```
### Ändring av loggnivåer i runtime
Här beskrivs två sätt att ändra loggnivåer i runtime (det finns förmodligen fler).

 1. Om en extern konfiguration används räcker det att ändra i konfigureringsfilen förutsatt att den är grundkonfigurerad att upptäcka förändringar i runtime. Kontrollera att parametern `monitorInterval="30"` är satt.
 2. Ändra loggnivåer med Hawtio (eller på annat sätt via jmx)
### Loggformat
VP loggar i json-format enligt Elastic Common Schema (ECS) för att underlätta indexering och sökning i logghanteringsverktyg som t.ex. ELK-stack eller Splunk.
Appenders bör använda `EcsLayout` för att få rätt format. Exempel på konfiguration av console appender med `EcsLayout`:
```xml
<Appenders>
    <Console name="ecs" target="SYSTEM_OUT">
        <EcsLayout/>
    </Console>
</Appenders>
```
### Rekommenderade loggers
Vissa loggers kan vara av extra intresse för att följa VPs uppstart och flöden. Se beskrivning nedan:
```xml
<Loggers>
     <!--Level INFO will log the init/reset of TAK cache-->
     <AsyncLogger name="se.skltp.takcache" level="INFO"/>
     <AsyncLogger name="se.skl.tp.vp.vagval.ResetTakCacheProcessor" level="INFO"/>
 
     <!--Level INFO will log the init/reset of HSA cache-->
     <AsyncLogger name="se.skl.tp.vp.service.HsaCacheServiceImpl" level="INFO"/>
 
     <!--Level INFO will log startup for spring boot application-->
     <AsyncLogger name="se.skl.tp.vp.VpServicesApplication" level="INFO"/>
 
     <!--Level INFO will log startup information for Camel -->
     <AsyncLogger name="org.apache.camel.spring.SpringCamelContext" level="INFO"/>
 
    <!-- Message logging
         Used to follow messages sent through VP
         Level DEBUG will log all message information including payload
         Level INFO will log all message information without payload
         See chapter "Meddelande loggning" for more information
    -->
    <AsyncLogger name="se.skl.tp.vp.logging.req.in" level="INFO"/>
    <AsyncLogger name="se.skl.tp.vp.logging.req.out" level="INFO"/>
    <AsyncLogger name="se.skl.tp.vp.logging.resp.in" level="INFO"/>
    <AsyncLogger name="se.skl.tp.vp.logging.resp.out" level="INFO"/>
 
    <!--Root logger-->
    <Root level="WARN">
       <AppenderRef ref="RollingRandomAccessFile"/>           
    </Root>
      
</Loggers>
```
### Meddelande-loggning
Det finns fyra speciella loggers som hanterar meddelanden som går genom VP.
   - INFO nivå - innebär att meddelanden loggas utan payload.
   - DEBUG nivå - innebär att meddelanden loggas med payload. Notera att payload kan innehålla känslig information och bör hanteras därefter.

Dessa kan individuellt slås av och genom att ställa upp loggnivån alternativt ta bort loggers.
 - se.skl.tp.vp.logging.req.in - Loggar inkommande meddelanden från konsumenten.
 - se.skl.tp.vp.logging.req.out - Loggar utgående meddelanden till producenten.
 - se.skl.tp.vp.logging.resp.in - Loggar svaret från producenten.
 - se.skl.tp.vp.logging.resp.out - Loggar svaret VP skickar till konsumenten.

Exempel på loggning av resp-out med payload:
```json
{
    "@timestamp": "2026-01-21T09:56:08.296Z",
    "log.level": "INFO",
    "destination.address": "localhost:19001",
    "destination.domain": "localhost",
    "destination.port": "19001",
    "event.action": "resp-out",
    "event.category": "[\"web\"]",
    "event.duration": "3000000",
    "event.id": "74c8659f-2180-4389-b4cd-73f8c9e1d07c",
    "event.kind": "event",
    "event.module": "skltp-messages",
    "event.type": "[\"access\",\"end\"]",
    "host.architecture": "amd64",
    "host.hostname": "192.168.1.2",
    "host.ip": "192.168.1.2",
    "host.os.family": "windows",
    "host.os.name": "Windows 11",
    "host.os.platform": "windows",
    "host.os.version": "10.0",
    "http.response.body.bytes": "16",
    "http.response.body.content": "<mocked answer/>",
    "http.response.headers": "{\"content-length\":\"16\",\"CamelHttpResponseCode\":\"200\",\"x-rivta-routing-history\":\"mock-producer\",\"CamelHttpResponseText\":\"OK\",\"connection\":\"keep-alive\",\"x-skltp-prt\":\"1\",\"Content-Type\":\"text/xml; charset=UTF-8\"}",
    "http.response.status_code": "200",
    "labels.originalServiceconsumerHsaid": "tp",
    "labels.receiverid": "HttpsProducer",
    "labels.rivversion": "rivtabp20",
    "labels.route": "vp-http-route",
    "labels.routerBehorighetTrace": "HttpsProducer",
    "labels.routerVagvalTrace": "HttpsProducer",
    "labels.senderid": "tp",
    "labels.servicecontract_namespace": "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1",
    "labels.wsdl_namespace": "urn:riv:insuranceprocess:healthreporting:GetCertificate:1:rivtabp20",
    "log.logger": "se.skl.tp.vp.logging.MessageInfoLogger",
    "message": "resp-out tp -> HttpsProducer",
    "service.name": "vp-services-test",
    "source.address": "127.0.0.1",
    "source.ip": "127.0.0.1",
    "span.id": "55045d4d-0986-49dd-979d-97f5d32b1425",
    "trace.id": "ae93bea6-b428-4ee2-813a-e0e78640d0a2",
    "transaction.id": "08FCEB55E529823-0000000000000017",
    "url.full": "http://localhost:12312/vp/PATH1%20",
    "url.original": "https://localhost:19001/vardgivare-b/tjanst2",
    "ecs.version": "1.2.0",
    "process.thread.name": "Camel Thread #16902 - NettyHttpsClient",
    "corr.id": "[ae93bea6-b428-4ee2-813a-e0e78640d0a2]"
}
```

[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)


   [log4j2s dokumentation]: <https://logging.apache.org/log4j/2.x/>
   [ecs-logging dokumentation]: <https://www.elastic.co/guide/en/ecs-logging/java/1.x/intro.html>
   [VP Camel konfigurering]: <configuration.md>
