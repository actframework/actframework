##### ACT Framework

The reason I create ACT Framework is because I was so in fond of PlayFramework1 V1.x and I want to continue along the road paved by the great product. In essence I need an expressive, Java developer friendly MVC framework, and that comes with ACT Framework.

Controller sample code:

```java
    @GetAction("/")
    public Result home() {
        return render();
    }

    @GetAction({"/hello", "/hi"})
    public String sayHello() {
        return "Hello Act!";
    }

    @GetAction("/bye")
    public void byePlayAndSpring() {
        text("bye Play and Spring!!");
    }

    @GetAction("/greeting")
    public void greeting(String who, int age) {
        render(who, age);
    }

    @GetAction("/product/{catalog}/{prod}/price")
    public Result price(String catalog, String prod) {
        int n = _.random(C.range(100, 400));
        String price = n + ".99";
        return render(catalog, prod, price);
    }
```

There are a couple of ACT Demo applications could be found at https://github.com/actframework/act-demo-apps, these apps demonstrate the following features of ACT Framework:

* Basic usage: https://github.com/actframework/act-demo-apps/tree/master/fullstack-app/helloworld
* Dependency injection with Guice: https://github.com/actframework/act-demo-apps/tree/master/fullstack-app/guice
* Configuration with code: https://github.com/actframework/act-demo-apps/tree/master/fullstack-app/app-config
* Jobs: https://github.com/actframework/act-demo-apps/tree/master/fullstack-app/jobs
* Validation (JSR 303, not fully completed): https://github.com/actframework/act-demo-apps/tree/master/fullstack-app/validation
* MongoDB with morphia: https://github.com/actframework/act-demo-apps/tree/master/fullstack-app/todo-morphia
* SQL DB with Ebean: https://github.com/actframework/act-demo-apps/tree/master/fullstack-app/todo-ebean
* Transactional support with Ebean: https://github.com/actframework/act-demo-apps/tree/master/fullstack-app/transaction-ebean

All the demo apps shall be able to run directly by `cd path/to/app` and `mvn clean compile exec:exec`. Then you can open your browser and navigate to `http://localhost:5460`. The demo apps are using default view based on [Rythm](http://rythmengine.org), which is my another open source contribution.

At the moment ACT framework is still under development and not ready for product use. But you are welcome to give it a try and send me feedback by [raising issues](/actframework/actframework/issues) or send email to actframework@googlegroup.com. 中国朋友可以申请加入ＱＱ群：283919975.

Happy coding!
