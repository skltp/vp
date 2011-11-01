#!/bin/bash

export VP_HOME="$( cd -P "$( dirname $0 )"/.. && pwd )"
export MULE_HOME=${VP_HOME}/mule-standalone-2.2.8

echo -n "Starting ActiveMQ 5.4.3..."
cd ${VP_HOME}/apache-activemq-5.4.3/bin
./activemq start &>/dev/null

if [ $? == 0 ]; then
	echo "done."
fi

echo -n "Starting Mule 2.2.8..."
cd ${MULE_HOME}/bin
./mule start -config vp-config.xml 2>&1 >/dev/null

if [ $? == 0 ]; then
	echo "done."
fi

exit 0

