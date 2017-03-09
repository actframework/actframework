# ACT Framework

[![Join the chat at https://gitter.im/actframework/actframework](https://badges.gitter.im/actframework/actframework.svg)](https://gitter.im/actframework/actframework?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## 新闻
* 2017-3-9 ACT 1.0.0 发布到maven中央库

## 项目状态

* 当前稳定版本: 1.0.0

## 安装

将下面的依赖添加进你的`pom.xml`文件

```xml
    <dependency>
      <groupId>org.actframework</groupId>
      <artifactId>act</artifactId>
      <version>[1.0.0, 2.0.0)</version>
    </dependency>
```

如果需要使用快照版发行,您还需要在`pom.xml`文件中加入下面的代码:

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
    * ActFramework基于[Genie](https://github.com/osglworks/java-di)的依赖注入是一个轻量且
    [高性能](https://github.com/greenlaw110/di-benchmark)的JSR330标准实现    
    * 在ActFramework使用Genie, 你不需要注册你的绑定模块, 只需要提供绑定模块的代码, ActFramework会自动注册

* **强大的单页/移动应用开发支持**
    * [JSON/RESTful支持](https://www.youtube.com/watch?v=B2RRSzYeo8c&t=4s)
    * [内置CORS支持](http://actframework.org/doc/configuration.md#cors)
    * 当不能使用Cookie的情况下, ActFramework提供了[回话/HTTP头映射](http://actframework.org/doc/configuration#session_mapper_impl)

* **必须的安全性**
    * 回话cookie设置为http only, secure(当运行在HTTPS上面时), 框架通过将cookie内容签名并加密(可选)来防止Cookie篡改
    * [只需一行配置即可启用CSRF保护](http://actframework.org/doc/configuration.md#csrf)
    * XSS防范: 缺省的Rythm模板引擎自动[将变量输出转码](http://fiddle.rythmengine.org/#/editor/398e71d927234f13a26bb346376141ce)
    * 采用[AAA plugin](https://github.com/actframework/act-aaa-plugin)实现认证/授权/记账机制

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
