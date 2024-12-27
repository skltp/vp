# VP
VirtualiseringsPlattformen (VP), byggd med Apache Camel. <br/>
[Mer information om VP](https://inera.atlassian.net/wiki/spaces/NTJPI/pages/3187247396/VP+Camel) <br/>

**Bygga projektet:<br/>**
_mvn clean install_<br/>

**Köra automatiska tester<br/>**
_mvn clean verify_<br/>

**För att starta projektet lokalt:**<br/>
För att alla VP-funktioner ska fungera måste man konfigurera i properties:
1. Länken till tak-services: _takcache.endpoint.address_
2. Filerna som innehåller tak-cache: _takcache.persistent.file.name_
3. Filerna som innehåller hsa-cache: _hsa.files_

Starta **VP** som Spring-boot applikation

**Configuration:**<br/>
[Konfiguration av VP Camel](doc/config/configuration.md)<br/>
[Detaljerad konfiguration](doc/config/detail_config.md)<br/>
[Logging konfigurering](doc/config/logging_configuration.md)