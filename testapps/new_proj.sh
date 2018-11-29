#!/usr/bin/env bash
mvn archetype:generate -B \
    -DgroupId=test \
    -DartifactId=GH$1 \
    -DarchetypeGroupId=org.actframework \
    -DarchetypeArtifactId=archetype-quickstart \
    -DarchetypeVersion=1.8.14.0