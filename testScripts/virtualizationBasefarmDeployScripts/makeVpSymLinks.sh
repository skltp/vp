#!/bin/bash
#-----------------------------------------------------------
# NTjP: Link all VP virtualization jars into the Basefarm
# setup dir that makes the jar's appear into VP's lib-dir.
#
# 2015-09-02 Hakan Dahl
#-----------------------------------------------------------

# first, manually: cd /www/inera/jar/mule/vp-services/
# AND put this script in the above dir
for f in /www/inera/releases/vp-services/* ; do
  echo "linking: ${f}"
  ln -s ${f} ${f##*/}
done
