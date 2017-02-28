# ACT Framework

[![Join the chat at https://gitter.im/actframework/actframework](https://badges.gitter.im/actframework/actframework.svg)](https://gitter.im/actframework/actframework?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## 新闻
* 一个ActFramework版本的[TodoBackend](http://www.todobackend.com/)项目: http://github.com/greenlaw110/todomvc-act
* Actframework 正式登录[码云](https://git.oschina.net/actframework/actframework)以及[开源中国](http://www.oschina.net/p/actframework)
* [TechEmpower Benchmark](https://www.techempower.com/benchmarks/)接受了
Actframework [性能测试项目](https://github.com/TechEmpower/FrameworkBenchmarks/tree/master/frameworks/Java/act) 
期待看到Act在即将来到的TechEmpower第14轮测试的表现
* 一个三方的[性能测试](https://github.com/networknt/microservices-framework-benchmark)显示Act在简单情况下的性能
 是Spring-boot的20倍

## 项目状态

* 当前稳定版本: 0.6.0-SNAPSHOT
* 第一个正式版预计在2017年三月发行

## 安装

将下面的依赖添加进你的`pom.xml`文件

```xml
    <dependency>
      <groupId>org.actframework</groupId>
      <artifactId>act</artifactId>
      <version>0.6.0-SNAPSHOT</version>
    </dependency>
```

因为现在还是使用快照版发行,您还需要在`pom.xml`文件中加入下面的代码:

```xml
  <parent>
    <groupId>org.sonatype.oss</groupId>
    <artifactId>oss-parent</artifactId>
    <version>7</version>
  </parent>
```

## 特性

* **全栈式MVC框架**
    * Actframework**不是**一个servlet框架. Act应用不需要servlet容器, 而是作为一个独立的Java应用程序运行. 
     一般可以在数秒之内启动

* **无与伦比的开发体验与高性能**
    * 一旦开始开发就无需重启服务器. Act的dev模式提供的热加载功能是每个Java开发员的梦想.  
      看看[这个三分钟的视频演示](https://www.youtube.com/watch?v=68Z-jTL6fDg)来体验一下把!
    * [这个第三方性能测试](https://github.com/networknt/microservices-framework-benchmark)展示了Act的强劲性能
      在简单的情况下Act可以获得Spring-boot20倍以上的吞吐量

* **完整的JSR330依赖注入支持**
    * ActFramework's DI support is built on top of [Genie](https://github.com/osglworks/java-di), a lightweight
      yet [fast](https://github.com/greenlaw110/di-benchmark) JSR330 implementation.
    * Benefit from Act's powerful class scan feature, it does not require the user to create injector from 
      modules (as the usually way you use Guice). Declare your module and your binding is automatically registered

* **Superb SPA/Mobile app support**
    * [Awesome JSON/RESTful support](https://www.youtube.com/watch?v=B2RRSzYeo8c&t=4s)
    * [Built-in CORS support](http://actframework.org/doc/configuration.md#cors)
    * [Session/Header mapping](http://actframework.org/doc/configuration#session_mapper_impl) so you are not limited to cookie

* **Uncompromising Security**
    * Session cookie is secure and http only, payload is signed and encrypted (optionally)
    * [Enable CSRF prevention with just one configuration item](http://actframework.org/doc/configuration.md#csrf)
    * XSS prevention: the default Rythm engine [escape variable output](http://fiddle.rythmengine.org/#/editor/398e71d927234f13a26bb346376141ce) by default
    * Implementing your authentication/authorisation/accounting framework using [AAA plugin](https://github.com/actframework/act-aaa-plugin)

* **Annotation aware but not annotation stack** 
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

* **Multi-environment configuration**
    * ActFramework supports the concept of `profile` which allows you to organize your configurations 
      in different environment (defined by profile) easily. Take a look at the following 
      configurations from one of our real project:
    
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
    Suppose on your UAT server, you start the application with JVM option `-Dprofile=uat`,
    ActFramework will load the configuration in the following sequence:
        1. Read all `.properties` files in the `/resources/conf/common` dir
        2. Read all `.properties` files in the `/resources/conf/uat` dir
    
    This way ActFramework use the configuration items defined in `uat` profile to overwrite 
    the same items defined in `common` profile. The common items that are not overwritten 
    still effective.

* **[Simple yet powerful database support](http://actframework.org/doc/model.md)**
    * [Multiple database support built in](http://actframework.org/doc/multi_db.md)

* **[Powerful view architecture with multiple render engine support](http://actframework.org/doc/templating.md)**

* **Commonly used tools**
    * [Sending email](http://actframework.org/doc/email)
    * [Schedule jobs](http://actframework.org/doc/job)
    * [Event handling and dispatching](http://actframework.org/doc/event)

## Sample code

### A HelloWorld app

```java
package demo.helloworld;

import act.Act;
import act.Version;
import org.osgl.mvc.annotation.GetAction;

public class HelloWorldApp {

    @GetAction
    public String sayHello() {
        return "Hello World!";
    }

    public static void main(String[] args) throws Exception {
        Act.start("Hello World", Version.appVersion(), HelloWorldApp.class);
    }

}
```

See [this 7 mins video on how to create HelloWorld in Eclipse from scratch](https://www.youtube.com/watch?v=_IhRv3-Ejfw).
or [for users without youtube access](http://www.tudou.com/programs/view/fZqqkFacfzA/)

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
        public User show(String id, Map<String, Object> data) {
            return findById(id);
        }

        @PutAction("{id}")
        public User update(String id, Map<String, Object> data) {
            User user = findById(id);
            notFoundIfNull(user);
            user.mergeValues(data);
            return save(user);
        }

        @DeleteAction("{id}")
        public void delete(String id) {
            deleteById(id);
        }
    }

    public static void main(String[] args) throws Exception {
        Act.start("RESTful Demo", Version.appVersion(), User.class);
    }

}
```

See [this 1 hour video on RESTful support](https://www.youtube.com/watch?v=B2RRSzYeo8c&t=4s) 
or [for user without youtube access](http://www.tudou.com/programs/view/K9ayRYIJNhk/)


See [this 7 mins video to understand more about AdaptiveRecord](https://www.youtube.com/watch?v=gWisqi-bp0M&t=1s)
or [for user without youtube access](http://www.tudou.com/programs/view/o4Up0B4wD8Y/)

## Background

I love PlayFramework v1.x because it is simple, clear and expressive. It brought us a completely different experience 
in web development with Java. However I don't totally agree with where Play 2.X is heading for, and it looks like I am 
not the only person with the concern as per this 
[open letter to Play Framework Developers](https://groups.google.com/d/msg/play-framework/AcZs8GXNWUc/IanbqC-c-MkJ). 

I [have thought of](http://software-lgl.blogspot.com.au/2012/12/thinking-about-creating-new-java-web.html) rolling out 
something that could follow the road paved by Play 1.x, something that is simple, clear, expressive and Java 
(specifically) developer friendly. About one and half year after that I decide I could start the project seriously, 
and now another one and half year passed by, I've got this ACT framework in a relatively good shape.

Happy coding!
