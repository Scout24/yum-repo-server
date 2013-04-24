#!/usr/bin/env bash
if [ ! -d $1 ] || [ -z $2 ]; then
	echo "Usage: $0 REPOBASEDIR TARGETHOST [--dry]" ; echo ; exit 0
fi

base=$1
host=$2
test "$3" == "--dry"
dry=$?

repos=$(find $base -name 'tags.txt' | cut -d '/' -f2)

for r in $repos; do 
	tags=$(cat $r/tags.txt | sort -u)
	for t in $tags; do
		if [ $dry -eq 0 ]; then
			echo "repoclient --hostname $host tag $r $t"
		else
			repoclient --hostname $host tag $r $t
		fi
	done
done
