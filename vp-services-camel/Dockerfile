FROM openjdk:11-oraclelinux8

ENV BASE_DIR=/opt/skltp/ \
    USER=ind-app \
    LOG_DIR=/var/log/ \
    JARFILE=target/vp-services-camel-*.jar

ADD ${JARFILE} ${BASE_DIR}/app.jar
RUN mkdir -p ${LOG_DIR} \
  && useradd -Ms /bin/bash -b ${BASE_DIR} -u 556559423 ${USER} \
  && chown ${USER}:${USER} -R ${BASE_DIR} ${LOG_DIR}

WORKDIR ${BASE_DIR}
USER ${USER}
CMD java -XX:MaxRAMPercentage=75 ${JAVA_OPTS} -jar app.jar