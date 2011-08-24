#!/bin/bash

echo -n "Stopping Mule 2.2.8..."
cd ~/vp/mule-standalone-2.2.8/bin
./mule stop -config vp-config.xml 2>&1 >/dev/null

if [ $? == 0 ]; then
	echo "done."
fi

echo -n "Stopping ActiveMQ 5.4.2..."
cd ~/vp/apache-activemq-5.4.2/bin
./activemq stop &>/dev/null

if [ $? == 0 ]; then
	echo "done."
fi

exit 0 
