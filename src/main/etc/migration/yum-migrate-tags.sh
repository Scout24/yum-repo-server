#!/usr/bin/env bash
if [ ! -d $1 ] || [ -z $2 ]; then
	echo "Usage:    yum-migrate-tags.sh REPOBASEDIR TARGETHOST [--dry]"
	echo
	echo "Example:  yum-migrate-tags.sh /data/repo/static/ new-yum.yourdomain.com --dry" 
	echo ; exit 0
fi

base=$1
host=$2
dry=$3

repos=$(find $base -name 'tags.txt' | rev | cut -d '/' -f2 | rev)

for r in $repos; do 
	tags=$(cat $base/$r/tags.txt | sort -u)
	for t in $tags; do
		if [ $dry == "--dry" ]; then
			echo "repoclient --hostname $host tag $r $t"
		else
			repoclient --hostname $host tag $r $t
		fi
	done
done
