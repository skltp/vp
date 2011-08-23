#!/bin/bash

echo -n "Starting ActiveMQ 5.4.2..."
cd ~/vp/apache-activemq-5.4.2/bin
./activemq start &>/dev/null

if [ $? == 0 ]; then
	echo " done."
fi

echo -n "Starting Mule 2.2.8..."
cd ~/vp/mule-standalone-2.2.8/bin
./mule start -config vp-config.xml 2>&1 >/dev/null

if [ $? == 0 ]; then
	echo " done."
fi

exit 0

