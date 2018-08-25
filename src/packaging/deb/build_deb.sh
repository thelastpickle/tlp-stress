#!/bin/bash

mkdir -p /deb/usr/{bin,lib}
mkdir -p /deb/etc
cp build/libs/tlp-stress-1.0-SNAPSHOT.jar /deb/usr/lib/
cp bin/tlp-stress /deb/usr/bin/

fpm -s dir -t deb -n tlp-stress -C /deb -m 'Jon Haddad' .
chown