# Livscykelhantering av Netty

Detta paket innehåller lokalt modifierade kopior av klasser från Nettys bibliotek `netty-codec-http`. Klasserna placeras i samma paket (`io.netty.handler.codec.http`) och läggs till i klasssökvägen före Nettys egna JAR-filer, vilket innebär att JVM:en laddar VP:s versioner i stället för originalet.

## Filer

| Fil                         | Syfte                                                                                                                                                                                    |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `HttpObjectDecoder.java`    | Hanterar svar där `Content-Length`-headern är större än det faktiska innehållet, genom att behandla "fixed-length"-svar på samma sätt som "variable-length"-svar i metoden `decodeLast`. |
| `HttpObjectAggregator.java` | Returnerar ett standardiserat VP015 SOAP-fault (HTTP 500) i stället för ettbart "413 Request Entity Too Large" när en inkommande konsumentförfrågan överskrider `vp.max.receive.length`. |

Originalfilerna sparas med suffixet `.original` (t.ex. `HttpObjectDecoder.java.4.1.132.original`) för att underlätta diff vid uppgradering.

## Livscykelhantering vid Netty-uppgradering

Kontrollera vilken version av `netty-codec-http` som används i projektet:
```powershell
mvn dependency:tree -pl vp-services-camel
```

Ladda ner källkods-JAR:en (`netty-codec-http-<version>-sources.jar`) från Maven Central (exempelvis https://repo1.maven.org/maven2/io/netty/netty-codec-http/4.1.132.Final/).
Extrahera de berörda klasserna till `.original`-filer med rätt versionssuffix. 

Jämför den gamla `.original`-filen med den nya för att se om Netty har ändrat de delar av koden som VP-modifieringarna berör.

Applicera VP:s lokala ändringar på den nya originalkoden.
