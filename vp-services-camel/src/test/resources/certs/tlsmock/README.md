# Certifikat för test

## CA

```sh
openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 -subj "/C=SE/O=Local Test Authority/CN=Local Test CA" -out ca.crt
```

## Servercertifikat

```sh
openssl genrsa -out server.key 2048
openssl req -new -key server.key -subj "/C=SE/O=WireMock Test/CN=localhost" -out server.csr
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 3650 -sha256 -extfile server-ext.cnf
```

## Klientcertifikat

```sh
openssl genrsa -out client.key 2048
openssl req -new -key client.key -subj "/C=SE/O=WireMock Client/CN=Test Client" -out client.csr
openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt -days 3650 -sha256 -extfile client-ext.cnf
```

## Verifiering

```sh
openssl verify -CAfile ca.crt server.crt
openssl verify -CAfile ca.crt client.crt

openssl x509 -in server.crt -noout -text
openssl x509 -in client.crt -noout -text
```

## Keystores och truststores

```sh
openssl pkcs12 -export -in server.crt -inkey server.key -certfile ca.crt -out server-keystore.p12 -name "server" -password pass:changeit
openssl pkcs12 -export -in client.crt -inkey client.key -certfile ca.crt -out client-keystore.p12 -name "client" -password pass:changeit
keytool -importcert -file ca.crt -alias localca -keystore client-truststore.jks -storepass changeit -noprompt
```

## Java Spring Boot-konfiguration

```yaml
spring:
  ssl:
    bundle:
      jks:
        client:
          keystore:
            location: "file:client-keystore.p12"
            password: "changeit"
            type: "PKCS12"
          truststore:
            location: "file:client-truststore.jks"
            password: "changeit"
```