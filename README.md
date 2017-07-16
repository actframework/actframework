# ACT Framework

[![Join the chat at https://gitter.im/actframework/actframework](https://badges.gitter.im/actframework/actframework.svg)](https://gitter.im/actframework/actframework?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Lecense](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](http://www.apache.org/licenses/LICENSE-2.0.html) [![Maven Central](https://img.shields.io/maven-central/v/org.actframework/act.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.actframework%22%20AND%20a%3A%22act%22)

## News
* 17/Jul/2017 [ACT 1.4.5 released](https://github.com/actframework/actframework/milestone/18?closed=1)
* 17/Jul/2017 [ACT 1.3.8 released](https://github.com/actframework/actframework/milestone/18?closed=1)
* 06/Jul/2017 [ACT 1.4.4 released](https://github.com/actframework/actframework/milestone/16?closed=1)
* 06/Jul/2017 [ACT 1.3.7 released](https://github.com/actframework/actframework/milestone/16?closed=1)
* 29/Jun/2017 [ACT 1.4.3 released](https://github.com/actframework/actframework/milestone/14?closed=1)
* 29/Jun/2017 [ACT 1.3.6 released](https://github.com/actframework/actframework/milestone/15?closed=1)
* 25/Jun/2017 [ACT 1.4.2 released](https://github.com/actframework/actframework/milestone/12?closed=1)
* 25/Jun/2017 [ACT 1.3.5 released](https://github.com/actframework/actframework/milestone/13?closed=1)
* 14/Jun/2017 [ACT 1.4.1 released](https://github.com/actframework/actframework/milestone/11?closed=1)
* 12/Jun/2017 ACT 1.4.0 released with [websocket support](http://actframework.org/doc/websocket.md)
* 11/May/2017 [TechEmpower benchmark R14](https://techempower.com/benchmarks/) published. 
    * Checkout ActFramework's performance in comparing with other full stack JVM web frameworks at [here](http://actframework.org/doc/techempower/r14)   
* 10/May/2017 ACT 1.3.3 released
* 05/May/2017 ACT 1.3.2 released
* 04/May/2017 ACT 1.3.1 released
* [02/May/2017 ACT 1.3.0 released](http://actframework.org/doc/releases/r1.3.0)
* [24/Apr/2017 ACT 1.2.0 released](http://actframework.org/doc/releases/r1.2.0)
* 17/Apr/2017 ACT 1.1.2 released
* 14/Apr/2017 ACT 1.1.1 released
* 10/Apr/2017 ACT 1.1.0 released
* 03/Apr/2017 ACT 1.0.7 released
* 27/Mar/2017 ACT 1.0.6 released
* 20/Mar/2017 ACT 1.0.3 released
* 13/Mar/2017 ACT 1.0.2 released
* 09/Mar/2017 ACT 1.0.0 released
* ACT 0.7.0-SNAPSHOT: rewrite JSR 303/349 Validation integration
* A [TodoBackend](http://www.todobackend.com/) project written in ActFramework: http://github.com/greenlaw110/todomvc-act
* Actframework 正式登录[码云](https://git.oschina.net/actframework/actframework)以及[开源中国](http://www.oschina.net/p/actframework)
* Actframework [benchmark set](https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/Java/act) 
accepted by [TechEmpower Benchmark](https://www.techempower.com/benchmarks/). Looking forward to
seeing Act in the 14 round test
* A 3rd party [simple benchmark project](https://github.com/networknt/microservices-framework-benchmark) 
shows Act's throughput is 20 times better than of Spring-boot in simple case

## Project status

- Current stable version: 1.3.3

## Install

Add the following dependency into your `pom.xml` file

```xml
<dependency>
  <groupId>org.actframework</groupId>
  <artifactId>act</artifactId>
  <version>${current-version}</version>
</dependency>
```

Add the following snippet into your `pom.xml` file if you want to get SNAPSHOT version:

```xml
<parent>
  <groupId>org.sonatype.oss</groupId>
  <artifactId>oss-parent</artifactId>
  <version>7</version>
</parent>
```

## Features

- **A full stack MVC framework**

  - Actframework is **NOT** a servlet framework. Act app does not run in a servlet container. Instead it run as an independent Java application and it starts in seconds

- **Unbeatable development experience w/ great performance**

  - Never restart your app when you are developing. Act's dev mode provides hot reloading feature makes it the dream of every Java web app developer. Check out [this 3 mins video](https://www.youtube.com/watch?v=68Z-jTL6fDg) and feel it!
  - According to [this 3rd party benchmark](https://github.com/networknt/microservices-framework-benchmark) Act beats most Java web framework on the market. In simple case Act can be 20 times faster than Springboot

- **Fully JSR330 Dependency Injection support**

  - ActFramework's DI support is built on top of [Genie](https://github.com/osglworks/java-di), a lightweight yet [fast](https://github.com/greenlaw110/di-benchmark) JSR330 implementation.
  - Benefit from Act's powerful class scan feature, it does not require the user to create injector from modules (as the usually way you use Guice). Declare your module and your binding is automatically registered

- **Superb SPA/Mobile app support**

  - [Awesome JSON/RESTful support](https://www.youtube.com/watch?v=B2RRSzYeo8c&t=4s)
  - [Built-in CORS support](http://actframework.org/doc/configuration.md#cors)
  - [Session/Header mapping](http://actframework.org/doc/configuration#session_mapper_impl) so you are not limited to cookie

- **Uncompromising Security**

  - Session cookie is secure and http only, payload is signed and encrypted (optionally)
  - [Enable CSRF prevention with just one configuration item](http://actframework.org/doc/configuration.md#csrf)
  - XSS prevention: the default Rythm engine [escape variable output](http://fiddle.rythmengine.org/#/editor/398e71d927234f13a26bb346376141ce) by default
  - Implementing your authentication/authorisation/accounting framework using [AAA plugin](https://github.com/actframework/act-aaa-plugin)

- **Annotation aware but not annotation stack**

  - Annotation is one of the tool ActFramework used to increase expressiveness. However we do not appreciate [crazy annotation stacked code](http://annotatiomania.com/). Instead we make the code to express the intention in a natural way and save the use of annotation whenever possible.

    For example, for the following SpringMVC code:

    ```java
    @RequestMapping(value="/user/{userId}/invoices", method = RequestMethod.GET)
    public List listUsersInvoices(
      @PathVariable("userId") int user,
      @RequestParam(value = "date", required = false) Date dateOrNull) {
        ...
    }
    ```

    The corresponding ActFramework app code is:

    ```java
    @GetAction("/user/{user}/invoices")
    public List listUsersInvoices(int user, Date date) {
      ...
    }
    ```

- **Multi-environment configuration**

  - ActFramework supports the concept of `profile` which allows you to organize your configurations in different environment (defined by profile) easily. Take a look at the following configurations from one of our real project:

    ```text
    resources
      ├── conf
      │   ├── common
      │   │   ├── app.properties
      │   │   ├── db.properties
      │   │   ├── mail.properties
      │   │   ├── payment.properties
      │   │   └── social.properties
      │   ├── local-no-ui
      │   │   ├── app.properties
      │   │   ├── db.properties
      │   │   └── port.properties
      │   ├── local-sit
      │   │   └── app.properties
      │   ├── local-ui
      │   │   ├── app.properties
      │   │   └── db.properties
      │   ├── sit
      │   │   ├── app.properties
      │   │   └── db.properties
      │   └── uat
      ...
    ```

    Suppose on your UAT server, you start the application with JVM option `-Dprofile=uat`, ActFramework will load the configuration in the following sequence:

    1. Read all `.properties` files in the `/resources/conf/common` dir
    2. Read all `.properties` files in the `/resources/conf/uat` dir

    This way ActFramework use the configuration items defined in `uat` profile to overwrite the same items defined in `common` profile. The common items that are not overwritten still effective.

- **[Simple yet powerful database support](http://actframework.org/doc/model.md)**

  - [Multiple database support built in](http://actframework.org/doc/multi_db.md)

- **[Powerful view architecture with multiple render engine support](http://actframework.org/doc/templating.md)**

- **Commonly used tools**

  - [Sending email](http://actframework.org/doc/email)
  - [Schedule jobs](http://actframework.org/doc/job)
  - [Event handling and dispatching](http://actframework.org/doc/event)

## Sample code

### A HelloWorld app

```java
package demo.helloworld;

import act.Act;
import act.Version;
import org.osgl.mvc.annotation.GetAction;

public class HelloWorldApp {

    @GetAction
    public String sayHelloTo(@DefaultValue("World") String who) {
        return "Hello " + who + "!";
    }

    public static void main(String[] args) throws Exception {
        Act.start("Hello World App");
    }

}
```

See [this 7 mins video on how to create HelloWorld in Eclipse from scratch](https://www.youtube.com/watch?v=_IhRv3-Ejfw). or [for users without youtube access](http://www.tudou.com/programs/view/fZqqkFacfzA/)

### A full RESTful service

```java
package demo.rest;

import act.controller.Controller;
import act.db.morphia.MorphiaAdaptiveRecord;
import act.db.morphia.MorphiaDao;
import org.mongodb.morphia.annotations.Entity;
import org.osgl.mvc.annotation.*;

import java.util.Map;

import static act.controller.Controller.Util.notFoundIfNull;

@Entity("user")
public class User extends MorphiaAdaptiveRecord<User> {

    @Controller("user")
    public static class Service extends MorphiaDao<User> {

        @PostAction
        public User create(User user) {
            return save(user);
        }

        @GetAction
        public Iterable<User> list() {
            return findAll();
        }

        @GetAction("{id}")
        public User show(@DbBind("id") User user) {
            return user;
        }

        @PutAction("{id}")
        public User update(@DbBind("id") @NotNull User user, Map<String, Object> data) {
            user.mergeValues(data);
            return save(user);
        }

        @DeleteAction("{id}")
        public void delete(String id) {
            deleteById(id);
        }
    }

    public static void main(String[] args) throws Exception {
        Act.start("RESTful Demo");
    }

}
```

See [this 1 hour video on RESTful support](https://www.youtube.com/watch?v=B2RRSzYeo8c&t=4s) or [for user without youtube access](http://www.tudou.com/programs/view/K9ayRYIJNhk/)

See [this 7 mins video to understand more about AdaptiveRecord](https://www.youtube.com/watch?v=gWisqi-bp0M&t=1s) or [for user without youtube access](http://www.tudou.com/programs/view/o4Up0B4wD8Y/)

## Background

I love PlayFramework v1.x because it is simple, clear and expressive. It brought us a completely different experience in web development with Java. However I don't totally agree with where Play 2.X is heading for, and it looks like I am not the only person with the concern as per this [open letter to Play Framework Developers](https://groups.google.com/d/msg/play-framework/AcZs8GXNWUc/IanbqC-c-MkJ).

I [have thought of](http://software-lgl.blogspot.com.au/2012/12/thinking-about-creating-new-java-web.html) rolling out something that could follow the road paved by Play 1.x, something that is simple, clear, expressive and Java (specifically) developer friendly. About one and half year after that I decide I could start the project seriously, and now another one and half year passed by, I've got this ACT framework in a relatively good shape.

Happy coding!
