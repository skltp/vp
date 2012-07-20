#!/bin/bash

export VP_BASE="$( cd -P "$( dirname $0 )"/.. && pwd )"
export VP_HOME=$VP_BASE/vp-home
export MULE_HOME=${VP_BASE}/mule-standalone-3.3.0
export JAVA_HOME=$VP_BASE/jdk1.6.0_33
# default for activemq is 1G (way too high)
export ACTIVEMQ_OPTS_MEMORY="-Xms384m -Xmx384m"

echo "Generates virtual service deployment descriptors..."
$JAVA_HOME/bin/java -jar $( dirname $0 )/vp-auto-deployer-1.0.jar $VP_HOME/vp/services/*-virtualisering-*.jar
if [ $? == 0 ]; then
        echo "done."
fi

echo -n "Starting ActiveMQ 5.6.0..."
cd ${VP_BASE}/apache-activemq-5.6.0/bin
./activemq start &>/dev/null

if [ $? == 0 ]; then
        echo "done."
fi

echo -n "Starting Mule 3.3.0..."
cd ${MULE_HOME}/bin
./mule start -config vp-config.xml 2>&1 >/dev/null

if [ $? == 0 ]; then
        echo "done."
fi

exit 0
