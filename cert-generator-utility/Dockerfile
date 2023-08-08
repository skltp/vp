FROM alpine

RUN apk add --no-cache openssl openjdk11-jre-headless
COPY generate_keystores.sh /usr/bin/keystoregen
RUN adduser -D usr1 \
 && chmod 755 /usr/bin/keystoregen
USER usr1
CMD /usr/bin/keystoregen