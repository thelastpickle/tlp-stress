#!/bin/bash

DEB_ROOT="build/deb"

# create hierarchy
rm -rf "$DEB_ROOT"
rm -rf *.deb

mkdir -p "$DEB_ROOT/usr/local/bin"
mkdir -p "$DEB_ROOT/usr/local/lib"
mkdir -p "$DEB_ROOT/etc/tlp-stress"

cp build/libs/tlp-stress-1.0-SNAPSHOT.jar "$DEB_ROOT/usr/local/lib/"
cp bin/tlp-stress "$DEB_ROOT/usr/local/bin/"
cp conf/logback-minimal.xml "$DEB_ROOT/etc/tlp-stress/logback.xml"

sed -i '5s/.*/TLP_STRESS_JAR="\/usr\/local\/lib\/tlp-stress-1.0-SNAPSHOT.jar"/' "$DEB_ROOT/usr/local/bin/tlp-stress"

fpm -s dir -t deb -n tlp-stress -C $DEB_ROOT -m 'Jon Haddad' -d openjdk-8-jre .
