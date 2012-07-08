#!/bin/bash

export VP_BASE="$( cd -P "$( dirname $0 )"/../vp && pwd )"
export VP_HOME=$VP_BASE/vp-home
export MULE_HOME=${VP_BASE}/mule-standalone-3.3.0
export JAVA_HOME=$VP_BASE/jdk1.6.0_33

echo -n "Stopping Mule 3.3.0..."
cd ${MULE_HOME}/bin
./mule stop -config vp-config.xml 2>&1 >/dev/null

if [ $? == 0 ]; then
        echo "done."
fi

echo -n "Stopping ActiveMQ 5.4.2..."
cd ${VP_BASE}/apache-activemq-5.4.2/bin
./activemq stop &>/dev/null

if [ $? == 0 ]; then
        echo "done."
fi

exit 0
