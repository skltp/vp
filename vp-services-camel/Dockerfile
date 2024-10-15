FROM eclipse-temurin:17-jre-alpine

ENV BASE_DIR=/opt/skltp/ \
    USER=ind-app \
    LOG_DIR=/var/log/ \
    JARFILE=target/vp-services-camel-*.jar

RUN apk add --no-cache jattach --repository http://dl-cdn.alpinelinux.org/alpine/edge/community/

ADD ${JARFILE} ${BASE_DIR}/app.jar
RUN mkdir -p ${LOG_DIR} \
  && adduser -HD -u 1000 -h ${BASE_DIR} ${USER} \
  && chown ${USER}:${USER} -R ${BASE_DIR}

WORKDIR ${BASE_DIR}
USER ${USER}
CMD java ${JAVA_OPTS} -jar app.jar
