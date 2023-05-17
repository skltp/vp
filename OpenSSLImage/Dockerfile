FROM alpine

RUN apk add --no-cache openssl openjdk11-jre-headless
COPY pkcs12_gen.sh /usr/bin/pkcs12gen
RUN adduser -D usr1 \
 && chmod 755 /usr/bin/pkcs12gen
USER usr1