# ActFramework Maven Archetypes

## Hello World App

A simple app that renders a home page with "Hello World". This application has end to end test cases provided. 

#### Copy/Paste and Go!

```
mvn archetype:generate -B \
    -DgroupId=com.mycom.helloworld \
    -DartifactId=helloworld \
    -DarchetypeGroupId=org.actframework \
    -DarchetypeArtifactId=archetype-quickstart \
    -DarchetypeVersion=1.9.1.0
```

#### Create Project interactively

```
mvn archetype:generate -DarchetypeGroupId=org.actframework -DarchetypeArtifactId=archetype-quickstart -DarchetypeVersion=1.9.1.0
```

## Hello Service

A Simple RESTful service scaffolding. This application has end to end test cases provided.

#### Copy/Paste and Go!

```
mvn archetype:generate -B \
    -DgroupId=com.mycom.helloworld \
    -DartifactId=helloworld \
    -DarchetypeGroupId=org.actframework \
    -DarchetypeArtifactId=archetype-simple-restful-service \
    -DarchetypeVersion=1.9.1.0
```

#### Create Project interactively

```
mvn archetype:generate -DarchetypeGroupId=org.actframework -DarchetypeArtifactId=archetype-simple-restful-service -DarchetypeVersion=1.9.1.0
```

## Bookmark

A full fledged RESTful service that managed bookmarks for multiple users. This app uses 
[act-aaa](https://github.com/actframework/act-aaa-plugin) to provide authentication/authorization support. This app also
leverage act-test framework for full covered end to end API level test.

#### Copy/Paste and Go!

```
mvn archetype:generate -B \
    -DgroupId=com.mycom.bookmark \
    -DartifactId=bookmark \
    -DarchetypeGroupId=org.actframework \
    -DarchetypeArtifactId=archetype-bookmark \
    -DarchetypeVersion=1.9.1.0
```

#### Create Project interactively

```
mvn archetype:generate -DarchetypeGroupId=org.actframework -DarchetypeArtifactId=archetype-bookmark -DarchetypeVersion=1.9.1.0
```

## Chatroom

A simple chatroom app that built on top of Websocket. This app also demonstrate how to do i18n in an Act application.

#### Copy/Paste and Go!

```
mvn archetype:generate -B \
    -DgroupId=com.mycom.chatroom \
    -DartifactId=chatroom \
    -DarchetypeGroupId=org.actframework \
    -DarchetypeArtifactId=archetype-chatroom \
    -DarchetypeVersion=1.9.1.0
```

#### Create Project interactively

```
mvn archetype:generate -DarchetypeGroupId=org.actframework -DarchetypeArtifactId=archetype-chatroom -DarchetypeVersion=1.9.1.0
```


