#!/bin/bash

d=`dirname $0`
$d/stop.sh
$d/start.sh

exit 0