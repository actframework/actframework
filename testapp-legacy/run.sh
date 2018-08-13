#!/bin/sh
mvn2 clean package
cd target/dist
unzip *.zip
./start &
cd ../..
mvn test
echo shutdown | nc localhost 6222
echo done
