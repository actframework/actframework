# ACT Framework

## News

* [A simple helloworld benchmark set](https://github.com/networknt/microservices-framework-benchmark)
* [A comprehensive TechEmpower benchmark set](https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/Java/act) has been [accepted](https://github.com/TechEmpower/FrameworkBenchmarks/pull/2525)

## Project status

* Current stable version: 0.5.0-SNAPSHOT
* The first public release is target to March 2017

## Install

Add the following dependency into your `pom.xml` file

```xml
    <dependency>
      <groupId>org.actframework</groupId>
      <artifactId>act</artifactId>
      <version>0.5.0-SNAPSHOT</version>
    </dependency>
```

Add the following snippet into your `pom.xml` file to get SNAPSHOT version:

```xml
  <parent>
    <groupId>org.sonatype.oss</groupId>
    <artifactId>oss-parent</artifactId>
    <version>7</version>
  </parent>
```

[![Join the chat at https://gitter.im/actframework/actframework](https://badges.gitter.im/actframework/actframework.svg)](https://gitter.im/actframework/actframework?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## News
* Actframework [benchmark set](https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/Java/act) 
accepted by [TechEmpower Benchmark](https://www.techempower.com/benchmarks/). Looking forward to
seeing Act in the 14 round test
* A 3rd party [simple benchmark project](https://github.com/networknt/microservices-framework-benchmark) 
shows Act's throughput 20 times faster than of Spring-boot in simple cases

## Features

* A full stack MVC framework
    * Actframework is **NOT** a servlet framework. Act app does not run in a servlet container. Instead
      it run as an independent Java application and it starts in seconds
* Unbeatable development experience w/ great performance
    * Never restart your app when you are developing it. Act's super dev mode provides hot reloading
      feature is the dream of every Java web app developer. Check out 
      [this 3 mins video](https://www.youtube.com/watch?v=68Z-jTL6fDg) to see it
    * According to [this 3rd party benchmark](https://github.com/networknt/microservices-framework-benchmark)
      Act's beats most of Java web framework. In simple case Act can be 20 times faster than Springboot
* Fully JSR330 Dependency Injection support
* Superb SPA/Mobile app support
    * [Awesome JSON/RESTful support](https://www.youtube.com/watch?v=B2RRSzYeo8c&t=4s)
    * [Built-in CORS support](http://actframework.org/doc/configuration.md#cors)
    * Session/Header mapping so you are not limited to cookie
* Uncompromising Security
    * Session cookie is secure and http only, payload is signed and encrypted (optionally)
    * [Enable CSRF prevention with just one configuration item](http://actframework.org/doc/configuration.md#csrf)
    * XSS prevention: Rythm engine [escape variable output](http://fiddle.rythmengine.org/#/editor/398e71d927234f13a26bb346376141ce) by default
    * Implementing your authentication/authorisation/accounting framework using [AAA plugin](https://github.com/actframework/act-aaa-plugin)
* Annotation aware but not annotation stack 
    * Annotation is one of the tool ActFramework used to increase expressiveness. However 
      we do not appreciate [crazy annotation stacked code](http://annotatiomania.com/). 
      Instead we make the code to express the intention in a natural way and save 
      the use of annotation whenever possible.
      
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

## Background

I love PlayFramework v1.x because it is simple, clear and expressive. It brought us a completely different experience in web development with Java. However I don't totally agree with where Play 2.X is heading for, and it looks like I am not the only person with the concern as per this [open letter to Play Framework Developers](https://groups.google.com/d/msg/play-framework/AcZs8GXNWUc/IanbqC-c-MkJ). 

I [have thought of](http://software-lgl.blogspot.com.au/2012/12/thinking-about-creating-new-java-web.html) rolling out something that could follow the road paved by Play 1.x, something that is simple, clear, expressive and Java (specifically) developer friendly. About one and half year after that I decide I could start the project seriously, and now another one and half year passed by, I've got this ACT framework in a relatively good shape.

Controller sample code:

```java
    @GetAction("/")
    public void home() {
    }

    @GetAction({"/hello", "/hi"})
    public String sayHello() {
        return "Hello Act!";
    }

    @GetAction("/bye")
    public Result byePlayAndSpring() {
        // use text to force response be "text/plain" content type
        return text("bye Play and Spring!!");
    }

    @GetAction("/greeting")
    public void greeting(String who, int age) {
        // if you didn't return render result, Act will throw it out 
        // as an Exception. Just like what Play!framework did 
        render(who, age);
    }

    @GetAction("/product/{catalog}/{prod}/price")
    public Result price(String catalog, String prod) {
        int n = $.random(C.range(100, 400));
        String price = n + ".99";
        return render(catalog, prod, price);
    }
```

## How to start

The easiest way to start your project is to copy one of the following ACT Demo applications which could be found at https://github.com/actframework/act-demo-apps

* Basic usage: https://github.com/actframework/act-demo-apps/tree/master/helloworld
* Dependency injection: https://github.com/actframework/act-demo-apps/tree/master/injection
* Configuration with code: https://github.com/actframework/act-demo-apps/tree/master/config
* Jobs: https://github.com/actframework/act-demo-apps/tree/master/jobs
* MongoDB with morphia: https://github.com/actframework/act-demo-apps/tree/master/todo-morphia
* SQL DB with Ebean: https://github.com/actframework/act-demo-apps/tree/master/todo-ebean
* Transactional support with Ebean: https://github.com/actframework/act-demo-apps/tree/master/transaction-ebean

All the demo apps shall be able to run directly by `cd path/to/app` and `mvn clean compile exec:exec`. Then you can open your browser and navigate to `http://localhost:5460`. The demo apps are using default view based on [Rythm](http://rythmengine.org), which is my [another open source initiative](http://github.com/greenlaw110/rythm).

At the moment ACT framework is still under development and not ready for product use. But you are welcome to give it a try and send me feedback by [raising issues](/actframework/actframework/issues) or send email to actframework@googlegroup.com. 中国的朋友可以申请加入ＱＱ群：283919975.

Happy coding!
