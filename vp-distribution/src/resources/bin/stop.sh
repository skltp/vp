#!/bin/bash

d=`dirname $0`/..
export VP_HOME=`pwd $d`
export MULE_HOME=${VP_HOME}/mule-standalone-2.2.8

echo -n "Stopping Mule 2.2.8..."
cd ${MULE_HOME}/bin
./mule stop -config vp-config.xml 2>&1 >/dev/null

if [ $? == 0 ]; then
	echo "done."
fi

echo -n "Stopping ActiveMQ 5.4.3..."
cd ${VP_HOME}/apache-activemq-5.4.3/bin
./activemq stop &>/dev/null

if [ $? == 0 ]; then
	echo "done."
fi

exit 0 
