# Logging konfigurering

Denna sida handlar om loggning. För allmän information om konfiguration av VP Camel, se [VP Camel konfigurering].
### Allmänt
Som logg-ramverk används Log4j2, se [log4j2s dokumentation] för mer information.
En grundkonfigurering finns i projektet under `resources/log4j2.xml` som default loggar till konsollen.

### Extern konfiguration
För att använda en extern log4j2.xml konfigurationsfil kan man starta applikationen med parametern `-Dlog4j.configurationFile`, 
till exempel:
 `java -jar -Xms256m -Xmx1024m -Dlog4j.configurationFile=file:///opt/vp/config/log4j2.xml vp-services.jar"`
### Ändring av loggnivåer i runtime
Här beskivs två sätt att ändra loggnivåer i runtime (det finns förmodligen fler), .

 1. Om en extern konfiguration används räcker det att ändra i konfigureringsfilen förutsatt att den är grundkonfigurerad att upptäcka förändringar i runtime. Kontrollera att parametern `monitorInterval="30"` är satt.
 2. Ändra loggnivåer med Hawtio (eller på annat sätt via jmx)
 ### Rekommenderade loggers
Vissa loggers kan vara av extra intresse för att följa VPs uppstart och flöden. Se beskrivning nedan:
```
log4j2.xml
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
         Used to follow messages sent thru VP
         Level DEBUG will log all message information including payload
         Level INFO will log all message information without payload
         See chapter "Meddelande loggning" for more information
    -->
    <AsyncLogger name="se.skl.tp.vp.logging.req.in" level="DEBUG"/>
    <AsyncLogger name="se.skl.tp.vp.logging.req.out" level="DEBUG"/>
    <AsyncLogger name="se.skl.tp.vp.logging.resp.in" level="DEBUG"/>
    <AsyncLogger name="se.skl.tp.vp.logging.resp.out" level="DEBUG"/>
 
    <!--Root logger-->
    <Root level="WARN">
       <AppenderRef ref="RollingRandomAccessFile"/>           
    </Root>
      
</Loggers>
```
### Meddelande-loggning
Det finns fyra speciella loggers som hanterar meddelanden som går genom VP.
   - INFO nivå - innebär att meddelanden loggas utan payload.
   - DEBUG nivå - innebär att meddelanden loggas med payload. 

Dessa kan individuellt slås av och genom att ställa upp lognivån alternativt ta bort loggers.
 - se.skl.tp.vp.logging.req.in - Loggar inkommande meddelanden från konsumenten.
 - se.skl.tp.vp.logging.req.out - Loggar utgående meddelanden till producenten.
 - se.skl.tp.vp.logging.resp.in - Loggar svaret från producenten.
 - se.skl.tp.vp.logging.resp.out - Loggar svaret VP skickar till konsumenten.

Exempel på loggning av resp-out med payload:
```
camel-app-vp.log
2019-05-24 10:57:10,695 DEBUG [Camel Thread #11 - NettyClientTCPWorker] se.skl.tp.vp.logging.resp.out  - skltp-messages
** logEvent-debug.start ***********************************************************
LogMessage=resp-out
ServiceImpl=vp-http-route
Host=ITEM-S12345.emea.msad.sopra (10.1.2.3)
ComponentId=vp-services-test
Endpoint=http://localhost:12312/vp/PATH1%20
MessageId=ID-ITEM-S67684-1558688206461-0-7
BusinessCorrelationId=0a50df13-eb1a-4386-b280-a86eafff17a7
ExtraInfo=
-servicecontract_namespace=urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1
-Headers={CamelHttpResponseCode=200, CamelHttpResponseText=OK, connection=keep-alive, content-length=16, x-skltp-prt=192}
-routerVagvalTrace==hsa-id-for-a-producer
-time.elapsed=4794
-originalServiceconsumerHsaid=tp
-source=se.skl.tp.vp.logging.MessageInfoLogger
-routerBehorighetTrace==hsa-id-for-a-producer
-senderIpAdress=127.0.0.1
-senderid=tp
-receiverid=hsa-id-for-a-producer
-endpoint_url=https://localhost:19001/vardgivare-b/tjanst2
-wsdl_namespace=urn:riv:insuranceprocess:healthreporting:GetCertificate:1:rivtabp20
-rivversion=rivtabp20
-time.producer=192
Payload=<test answer from testproducer/>
```

[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)


   [log4j2s dokumentation]: <https://logging.apache.org/log4j/2.x/>
   [VP Camel konfigurering]: <configuration.md>
