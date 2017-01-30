#!/bin/sh
mvn2 clean package
cd target/dist
unzip *.zip
./start
