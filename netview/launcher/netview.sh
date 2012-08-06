#!/bin/bash
set -e

NVHOME="./nvhome"

SCRIPT=`readlink -f $0`
JARDIR=`dirname $SCRIPT`

if [ ! -z $1 ]; then
	NVHOME="$1"
fi

if [ ! -e $NVHOME ]; then
	mkdir -p $NVHOME
fi

cd $NVHOME
java -jar $JARDIR/netview.jar

