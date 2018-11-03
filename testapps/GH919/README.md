# GH919 API doc - it shall support `@Sensitive` annotation

## Reproduce

Run `./run_dev`. Once app started it will print error logs like:

```
2018-11-03 09:39:00,604 WARN  a.j.Job@[jobs-thread-6] - error executing job compile-api-book
com.alibaba.fastjson.JSONException: write javaBean error, fastjson version 1.2.47, class gh919.AppEntry$Foo
	at com.alibaba.fastjson.serializer.JavaBeanSerializer.write(JavaBeanSerializer.java:465)
	at com.alibaba.fastjson.serializer.JavaBeanSerializer.write(JavaBeanSerializer.java:120)
	at com.alibaba.fastjson.serializer.JSONSerializer.write(JSONSerializer.java:281)
	at com.alibaba.fastjson.JSON.toJSONString(JSON.java:591)
	at com.alibaba.fastjson.JSON.toJSONString(JSON.java:580)
	at com.alibaba.fastjson.JSON.toJSONString(JSON.java:740)
	at act.apidoc.Endpoint.generateSampleJson(Endpoint.java:474)
	at act.apidoc.Endpoint.explore(Endpoint.java:337)
	at act.apidoc.Endpoint.<init>(Endpoint.java:213)
	at act.apidoc.ApiManager$2.visit(ApiManager.java:148)
	at act.route.Router.visit(Router.java:193)
	at act.route.Router.visit(Router.java:199)
	at act.route.Router.accept(Router.java:180)
	at act.apidoc.ApiManager.load(ApiManager.java:143)
	at act.apidoc.ApiManager.load(ApiManager.java:112)
	at act.apidoc.ApiManager$1.run(ApiManager.java:84)
	at act.job.Job$4.apply(Job.java:394)
	at act.job.Job.doJob(Job.java:355)
	at act.job.Job.run(Job.java:292)
	at act.job.Job$LockableJobList$1.run(Job.java:96)
	at act.job.JobManager$ContextualJob$1.apply(JobManager.java:483)
	at act.job.Job.doJob(Job.java:355)
	at act.job.Job.run(Job.java:292)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$201(ScheduledThreadPoolExecutor.java:180)
	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:293)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
Caused by: java.lang.reflect.InvocationTargetException: null
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at com.alibaba.fastjson.util.FieldInfo.get(FieldInfo.java:484)
	at com.alibaba.fastjson.serializer.FieldSerializer.getPropertyValueDirect(FieldSerializer.java:140)
	at com.alibaba.fastjson.serializer.JavaBeanSerializer.write(JavaBeanSerializer.java:249)
	... 29 common frames omitted
Caused by: org.osgl.exception.UnexpectedException: java.lang.IllegalArgumentException: contains illegal character for hexBinary: M4yjB9b&SPc5f&!K
	at org.osgl.util.E.unexpected(E.java:189)
	at org.osgl.util.Crypto.decryptAES(Crypto.java:361)
	at act.crypto.AppCrypto.decrypt(AppCrypto.java:135)
	at gh919.AppEntry$Foo.getName(AppEntry.java)
	... 36 common frames omitted
Caused by: java.lang.IllegalArgumentException: contains illegal character for hexBinary: M4yjB9b&SPc5f&!K
	at javax.xml.bind.DatatypeConverterImpl.parseHexBinary(DatatypeConverterImpl.java:451)
	at javax.xml.bind.DatatypeConverter.parseHexBinary(DatatypeConverter.java:357)
	at org.osgl.util.Codec.hexStringToByte(Codec.java:220)
	at org.osgl.util.Crypto.decryptAES(Crypto.java:345)
	... 38 common frames omitted
```
