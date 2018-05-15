#!/bin/bash

sbtver=0.13.17
sbtdir=sbt_launch
sbtbin=./$sbtdir/sbt/bin/sbt

sbtrepo=https://sbt-downloads.cdnedge.bluemix.net/releases/v1.1.5
sbtzip=sbt-1.1.5.zip

if [ ! -f $sbtbin ]; then
  echo "downloading $sbtjar" 1>&2
  if ! curl --location --silent --fail --remote-name $sbtrepo/$sbtzip; then
    exit 1
  fi
  unzip $sbtzip -d $sbtdir
fi

[ -f ~/.sbtconfig ] && . ~/.sbtconfig
$sbtbin "$@"
