# ActFramework Change Log

**1.8.8**
* Add `notEqual` e2e verifier #785
* Add `attachmentName()` method to `ActionContext` #783
* Extend View framework to support direct rendering of non-text content without template #782
* `java.util.NoSuchElementException: null` issue when force respond `csv` type #781
* Allow it specify `private` in `@CacheFor`for `Cache-Control` directive #780
* Merge act-e2e into act core framework #779
* Generate etag for `@CacheFor` request handlers #778
* Upload always failed after the first time #776
* Support simplified mail template path #774
* `PasswordSpec` - allow `null` value for password validation #773
* `@InvokeAfter` does not work #772
* Support gradle/Java project structure #771
* Add `PrincipalProvider` mechanism #770
* Add annotation to mark creator/modifier of an entity #769
* `AppClassLoader` - return enhanced bytecode for `getResource` call #768
* Improve Singleton detection for controller instance creation #767
* JsonDto class generation - support request handler reuse #766
* API Doc - Support request handler reuse #765
* Error starting app when `cacheFor.dev` set to `true` #760
* @DateFormatPattern has no effect on `Date` field #759
* `ResourceLoader` - load files in directory into `Map` structure #758
* Enhance `ResourceLoader` #757
* Missing Chinese message for some validation violations #755
* `@LoadResource` cannot inject resource content into Map or Properties #753
* JSON string return issue when Accept is not specified #752
* It generates 10 controller instances when app starts up #751
* Remove `undertow-websockets-jsr` dependency #749
* It shall send back 404 if return File does not exist #748
* `ActUnauthorized` - source info shall be populated upon each call #745
* Error page: error highlight line not matched the line triggered the runtime error #744
* Improve SYSTEM_SCAN_LIST matching performance #743
* Add `App.wasStarted()` method #742
* `JobAnnotationProcessor` issue: non static method in abstract class shall not be treated as static #741
* Compiler error shall print out source code and the line that break compiling in dev mode #735
* Improve JSON output of String type result #739
* Support returning JSON response in nonblock mode #738
* improvement for checking singleton object
* `SampleDataCategory` - make aliases() method be public
* `@Configuration` field shall be stateless for a Controller #734
* AdaptiveRecord enhancement #733
* Password enhancement - ignore the case when password is empty #730
* Register TypeConverter automatically #728
* Ajax redirect callback not get called when there are parsing error for JSON request #727
* Add TypeConverter from String to Joda datetime #726
* Large static resource output failure #725
* `job already registered` error when multiple job annotation put on the same method #724
* Make `ClassInfoRepository` injectable #723
* Provide a mechanism to allow developer easily define global FastJSON serializer and deserializer #722
* Provide built-in endpoint to fetch Enum i18n strings #721
* Add API doc to act aaa authentication list #720
* `PostHandle` event not raised in rare case #719
* forward shall not raise `PreHandle` event #717
* API doc `generateSampleData` - `fastJsonPropertyPreFilter` treatment for collection type #716
* session id not serialized in JWT token #711
* Act session token still validate even when user has logout #702
* Act start failed when set session.mapper=act.session.HeaderTokenSessionMapper #701
* `NullPointerException` encountered when app started with SQL db plugin without model classes #707
* Allow use app name and version as server header #710
* API doc - provide a mechanism allow plugin sample data generator #709
* Create an annotation to force application to add `DisableCircularReferenceDetect` to FastJSON #706
* Add `DisableCircularReferenceDetect` feature to FastJSON by default #705
* When `Accept` is not specified or unknown then use JSON as the output content type #704
* Make `SessionFree`, `NonBlock` and `SkipBuiltInEvents` annotation support at class level #703
* ACT-1.8.8-RC11 performance issue with Fortunes mustache templates #698
* Add pass-through mode to wave session resolution #697
* Allow developer to configure app cache default TTL #696
* Provide a mechanism to disable CAPTCHA protection #694
* Allow binding epoch time millis directly to date types #691
* `ActionContext.allowIgnoreParamNamespace` not effective when POJO has collection typed fields #690
* ParamLoader: POJO array or container field shall be initialized even if no request data for them #689
* JWT token expiration is not effect #688
* Customized `StringValueResolver` not work #687
* JobContext: support parent #686
* Job - emit events when JobContext created or destroyed #685
* Param binding - need to treat `@Password` annotated field #684
* Classloading: it shall automatically add inner class into management for managed class #683
* Classloading: the logic to determine classes eligible to enhancement is not clear #682
* Parameter binding: bind an ID string to an entity field #681
* Revert changes to "#429" #680
* Update to osgl-mvc 1.8.0 to fix #678
* Fix the logic on output response through a Writer to support the fix of #676
* `RenderCSV` shall check `Result.stringContentProducer` #677
* Add API to `Act` to support shutdown Act with an optional exit status code #673
* Param loading process shall take OSGL global mapping filter into consideration #672
* It shall keep original config key string while processing it into canonical form #669
* Print out `API book compiled` when running in dev mode #668
* It shall ignore Exception raised during app shutdown process #667
* It shall clear OSGL Config's internal cache on app restart #666
* update osgl library dependencies
    - osgl-tool to 1.18.0
    - osgl-cache to 1.5.0
    - osgl-genie to 1.8.0
    - osgl-http to 1.8.0
    - osgl-mvc to 1.8.0
    - osgl-storage to 1.7.0
    - osgl-tool-ext to 1.2.0
* Restructure response processing logic #665
* Major performance degrade in latest TFB test #663
    - Get rid of ActContext.strBuf. It is not really useful and allocate too much memory
    - Make header overwrite by request parameters an optional feature
* update osgl-tool to 1.12.0, osgl-http to 1.6.1, osgl-mvc to 1.7.0
* Add `getResource(String)` and getResourceAsStream(String)` method to `Act` and `App` #662
* API doc - it shall change the styling for print media #608
* update riotjs to 3.10, jquery to 3.3.1
* Clean up extra error logs #661
* A weird `ClassCastException` #660
* Add helper methods to `LogSupport` for line printing #659
* Allow set delay time for `@OnAppStart` and `@AlongWith` #658
* `echo` handler shall set `Content-Type` header #650
* `ActionContext.loginAndRedirect` does not trigger redirect to happen #648
* Add act version info in the error page in dev mode #639
* JSON Writer shall use default date/time pattern setting when i18n is not enabled - related to #645
* Enum resolving - support non-exact keyword based matching #643
* Enum resolving - it shall respond with `400 Bad Request` if resolving failed #642
* `NullPointerException` while resolving `int[]` from Query parameter #641
* Support bind to Cookie directly #638
* Dependency Injection with generic type #637
* Support passing settings via environment variable #636
* Use canonical property key #635
* `scan_package` setting in configuration file is not effective #634
* Passing multiple packages in `Act.start(String, String)` trigger `IllegalArgumentException` #633
* Support getting Header value from query parameters #631
* `H.Request.current()` returns `null` #628
* `render(...)` renders `__arg_names__` in the final result for JSON type output #626
* Support different date time format for different locale #607
* `me.tongfei.progressbar` runs on Java8 only #622
* Session expiration time shall not be output using `Expires` header #623
* Param binding: allow it to ignore missing namespace when there is only one param to bind #618
* @On(async = true) not work #611
* Issue with global URL context setting #614
* Date format not applied when returned type is `DateTime[]` #610
* DateTime return result not formatted as configured #604
* DateTime return result is not a valid JSON string #605
* Response Headers contains unreadable code #601
* Make `Router` injection support `@Named` #603
* It shall inject active Router instead of default router #602
* JSON response for array type result is not consistent #595
* System builtin route mapping added with wrong route source #597
* It shall not allow application to overwrite built-in service route mapping #598

**1.8.7** 06/Apr/2018
* `@PropertySpec` annotation not effective in API doc generated sample data #594
* Duplicated class/attribute in endpoint will be ignored #593
* update osgl-tool to 1.10.0
* update osgl-genie to 1.7.0

**1.8.6** 04/Apr/2018
* built_in_req_handler.enabled` configuration issue #590
* It does not print warning message for app without secret configured #592
* API Doc - failed to generate sample data for List typed embedded data structure #591
* API Doc - make it able to hide system endpoints #589
* API doc - set maximum length for the description width #588

**1.8.5** 02/Apr/2018
* Support output binary content by returning `byte[]` #586
* It cannot direct output text or binary content #585
* update osgl-tool to 1.9.0

**1.8.4** 28/Mar/2018
* API doc pattern compilation error on Java7 #584
* API doc enhancement - support markdown syntax in description #579

**1.8.3** 27/Mar/2018
* `CliServer` - allow site local ip to access #577

**1.8.2** 25/Mar/2018
* update osgl-tool to 1.8.2
* update osgl-http to 1.5.1
* update osgl-mvc to 1.5.3
* update osgl-cache to 1.3.2
* update osgl-logging to 1.1.2
* Allow output session token expiration time through response header #575
* API Doc - parsing JavaDoc to generate descriptive content #576
* API doc index not correct #573
* Use nonblocking I/O for body parsing when possible #572
* Improve the performance of direct output model #568
* Revert response output framework for small response #567
* `NoSuchMethodError` with Morphia entity #565
* API Doc error during app start #564
* Different URL variable name caused duplicate routes not been reported #561
* `ActUnauthorized` caused `java.lang.NegativeArraySizeException` in dev mode #563
* Download file not working properly #562
* Error with sending large response: UT000043: Data is already being sent. You must wait for the completion callback to be be invoked before calling send() again #560
* ResourceLoader: support loading json file into Map or other POJO #559
* Support `@DefaultValue` with `@Configuration` #558
* `StackOverflowError` encountered when `SimpleBean` field name does not follow Java convention #546


**1.8.1** 11/Mar/2018
* Support request forward #556
* Cannot apply text result to format: javascript #555
* Random `NullPointerException` on act-1.6.6 #553
* update commons.fileupload to 1.3.3 #549
* Ebean <model> is not enchaned? #545
* NPE running feature-test sample project #544
* if use multiple datasource,throw class registered warning/error when app start #468

**1.8.0** 4/Mar/2018
* update osgl-tool to 1.7.0
* update osgl-genie to 1.5.0
* update osgl-http to 1.4.0
* update osgl-mvc to 1.5.0
* update undertow to 1.4.23.Final
* By default enable CLI on PROD mode
    - we will disable CLI in the high security starter kit by default. 
* Make @Configuration working on publice static properties #536 
* `MasterEntityMetaInfoRepo` - support `MappedSuperClass` annotation registration #543
* Add `NamedProvider` for `CacheService` #542
* `ProjectLayout` - provide layout for gradle_groovy combination #541
* Cannot run a groovy program even in PROD mode #540
* Implement new response output model #539
* Param binding failure for `List<Foo>` style parameters #538
* notFoundIfNull return Content-Type as text/html #537

**1.7.3**
* Fix regression issue with #504

**1.7.2** 
* EntityMetaInfo framework - Support capture EntityListener annotation #535
* Add `TimestampGenerator` for `java.sql.Time` and `java.sql.Timestamp` #534
* Cannot call Password.Verifier.verifyPassword on object without a @Password field #528
* Running `Config` sample project in prod mode cannot find template #527
* Large content sent to response get truncated #531
* Extends error response to allow user defined code #530
* Add `Controller.Util.download(URL)` helper method #529

**1.7.1** 21/Feb/2018 
* `EventBus.Key.effectiveTypeOf` cause stackoverflow in Event sample #525
* NPE running config sample #524
* Allow normal action handler support partial path #523

**1.7.0** 19/Feb/2018
* Update JPA api to 2.2
* EntityMetaInfo and scanner - support JPA plugin
* Update to act-asm-5.0.3 for precise line number in error reporting
* Improve built-in service performance by make them as nonblock when possible
* Support `DirectoryIndex` for file `ResourceGetter` and `FileGetter` #521
* rename configuration `cli.session.expiration` to `cli.session.ttl` to make it comply to `session.ttl` configuration
* testapp apidoc error #519
* Simple event dispatching failed when event listener method argument list contains Interface #518
* `Env.RequireMode` not working on route registration #517 
* `ProgressGauge` API changes #516
* `JsonView` on class has no effect to CLI command #515
* `DefaultSessionCodec.processExpiration` error #513
* JWT: sometimes JWT deserialization failed #512
* Command line param binding failed for `char[]` #511
* Make API doc TOC organised by HTTP method and URL path #510
* `NullPointerException` after app reloaded from an ASM error in dev mode #509
* Error page not displayed if asm error raised during scanning phase #508
* `@SessionVariable` binding failure #506
* `@DefaultValue` not working for primitive types #504
* Create `act.util.CsvView` as an alias of `ResponseContentType(H.MediaType.CSV)` #503
* Add suffix to download file for csv typed response #502
* ResourceLoader - support any object type #497
* `ClassCastException` with Ehcache #495
* Further simplify password field processing #491
* Encrypt sensitive data in persistent storage #490
* Do not `Set-Cookie` for session and flash if there is no state #484
* Make `act.metric.Timer` be a `Closable` #483
* Simplify measuring of method execution #482
* DB: add annotation to mark created and last modified timestamp fields #480
* Missing actframework version in the banner text in dev mode with Eclipse project #478
* Rename `AppEvent` to `SysEvent` #475
* Implement a rotation secret for Act application #474
* Improve simple event handler mechanism #473
* Emit/trigger `EventObject` typed event through `EventBus #472
* SimpleEventHandler mechanism does not work with `EventObject` #471
* Allow `LoadResource` inject into Set of strings #470
* Param binding - it shall not try to get provider for simple types #449
* Support plugin JAX-RS #448
* Add method annotation to direct framework use `JSON.toJSONStringWithDateFormat()` #281
* Trace handler call #238
* Make it able to specify the named ports in routes configuration #183

**1.6.6 23/Jan/2018
* `context.username()` returns `null` after upgrade act to 1.6.4 #485

**1.6.5 23/Jan/2018
* `job.list` system command not working #481
* Update fastjson to 1.2.45

**1.6.4 14/Jan/2018**
* Performance issue with Rythm #469
    - update rythm-engine to 1.2.2
* Other performance updates
    - in act core code
    - update osgl-genie to 1.3.4
    - update osgl-http to 1.2.3

**1.6.3 13/Jan/2018**
* Template not reload after changed in `dev` mode #467
* ApiManager prints a lot of warning messages #466
* `ClassCastException` caused by `@CacheFor` #465
* Show entry URL on the console #463
* It displays control characters in the Eclipse console #462
* Limit the access to CLI service #464

**1.6.2 11/Jan/2018**
* Add string resolver for `java.sql.Date` and `java.sql.Timestamp` #460
* Make default Date format be date instead of date and time #459
* Make `ResourceLoader` support URL type #457
* Fault response for `txt/plain` response #456
* Turn off `@CacheFor ` on `dev` mode #455
* Support `X-Forwarded-For` to allow app get real remote ip when app is behind a reverse proxy #454
* Customized `EbeanConfigLoaded` event listener not triggered #453
* Param binding failure for `java.sql.TimeStamp` typed parameter #452
* Allow it configure the first time the `@Every` job be invoked #451
* `@Every` without specifying the time failed #450
* Param binding - it shall not try to get provider for simple types. #449
* Some view engine caused browser always loading when running in prod mode #447
* rythm tag @resource @asset generated path shall start with `/` #445

**1.6.1 06/Jan/2018**
* Router - avoid regex matching when possible #442
* Make `@JsonView` annotation an alias of `@ResponseContentType(H.Media.JSON)` #440
* Support Content-Security-Policy header #439
* Support using MACRO for URL path regex definition #438

**1.6.0 28/Dec/2017**
* Update osgl-tool to 1.5.2
* Update osgl-genie to 1.3.3
* Update fastjson to 1.2.44
* Fix logic error in `DefaultSessionCodec` - session timeout processing not effective
* Fix a few issues relevant to configuration loading

**1.6.0-RC2 19/Dec/2017**
* Update Version to osgl-version-2

**1.6.0-RC1 19/Dec/2017**
* Support throttle control #435
* Enhance `@Configuration` injection #434
* css resource `Content-Type` not set in prod mode #430
* ParamLoader: POJO instance not intialized if no field is set #429
* Support loading `AdaptiveRecord` from form post data #428
* java.lang.ClassCastException when ACT startup with session.ttl configuration #427
* Support SerializeFilter when return entity through FastJSON #426
* `@TemplateContext` shall inherit from parent class #424
* `@Configuration` not works properly #423
* Support `@Configuration` inject #421
* Create an mechanism to load resource from config folder #420
* Support annotated routing directive #419
* Error encountered during app startup on JDK 7 #418
* Add `findLatest()` method to `Dao` #402
* Support inject file content into `String` or `ByteBuffer` #397
* New static file/resource handler that are subject to authentication/session management #396
* Created.withLocation shall generate valid JSON string when `Accept` is `application/json` #349
* Cache template loaded in ViewManager #348
* use underscore style token for enum class name in enum i18n key #333
* Support webjar #331
* Support dynamic URL path variables #325
* Add timestamp to default error response #274
* Allow `AppJobManager.now` API accept job name parameter #268
* support different redirect semantic #263
* Request to support versioning of static resource mappings #210
* Support inline template #289
* Exception using underscore in a URL path variable name #295

**1.5.3 18/Nov/2017**
* NPE encountered when CSRF protector redirect request to login page #415
* Evict original URL cache from session once redirection happened #414
* update fastjson to 1.2.41 #416

**1.5.2 17/Nov/2017**
* Handle login redirect gracefully #412
* `NPE` while returning an `InputStream` in an action handler #410
* Mal behavior of app running in dev mode with `.version` file #409

**1.5.1 15/Nov/2017**
* `NullPointerException` when invoking job #407

**1.5.0 15/Nov/2017**

* Inject annotation of the handler method into the interceptor param list #406
* Add built-in API to report app version and act version #405
* Update riotjs version and add riot-route.js #404

**1.4.14 07/Nov/2017**

* File download not working correctly #401
* `NullPointerException` encountered with WebSocket Connection Event #400
* Better support for app to directly write to content to response #399
* `UT000002: The response has already been started` when it write content to response #398
* Send back `400 Bad Request` on response trigger an obscure error page when content type is `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` #394

**1.4.13 16/Oct/2017**

* Update riotjs to 3.7.2
* Add `createWebSocket(path)` method to `jquery.ext.js` #392
* The `ActContextProvider` does not favor `WebSocketContext` type #391
* Allow websocket message handler to return object #390
* It shall set `WebsocketContext` thread local variable upon ws message incoming #389
* Multiple `@Catch` can not work normally #388
* jquery.ext.js: allow `jQuery.put()` and `jQuery.putJSON()` method to use raw body #387
* `Cannot find out Dao for model type[class act.db.ModelBase]` issue #386
* Prevent interceptor method from binding JSON data by default #385
* render.json.output_charset configuration not working on POST request #384
* ACT can only map wrapped JSON object to entity #383
* Allow developer to disable actframework built-in routes #382
* when loading app from jar file it shall set `app.mode` to `prod` #381
* Add `sessionId()` method to `ActContext` #380
* Allow app to specify date time format to parse remote JSON response #379
* It always return `null` when executing CLI command that handled by a static method #378
* Allow it to skip implicity view arguments for certain controller method #376
* Properties files in jar are ignored #375
* Publish backend job progress through websocket #356
* CLI progress report: it reports less than 100 percent when job finished sometimes #324

**1.4.12 13/Sep/2017**

* `SequenceNumberGenerator` cause error in heterogeneouse data source environment #374
* Allow inject `Dao` interface #373

**1.4.11 10/Sep/2017**
* catch up update to 1.3.14-LTS
* Support running CLI Job in background #267 
* `job.list` CLI command failure #355 

**1.4.10 21/Aug/2017**
* catch up update to 1.3.13-LTS

1.4.9
* catch up update to 1.3.12-LTS

1.4.8
* catch up update to 1.3.11-LTS

1.4.7
* catch up update to 1.3.10-LTS

1.4.6
* Catch up update to 1.3.9-LTS

1.4.5
* Catch up update to 1.3.8

1.4.4
* Catch up update to 1.3.7

1.4.3
* Two `WebSocketConnectionManager` exists #250 
* HttpServerExchange cannot have both async IO resumed and dispatch() called in the same cycle #248 
* catch up to bug fixes in 1.3.6

1.4.2
* catch up to bug fixes in 1.3.5

1.4.1
* `DbServiceManager.hasDbService()` error implementation #239 

1.4.0
* Update fastjson to 1.2.33 #235 
* App start event listener not called when there is no Async DbService #234 
* Generate ASCII banner for favicon #228 
* Support colorful console output #227 
* Support customized banner text #226 
* Allow app to terminate `@InheritedStateless` #223 
* Make `Dao` implementation be stateless #221 
* Automatically register a class with `@Stateless` tag into app's singleton registry #220 
* Support Lazy initialized singleton #219 
* Support initialize DbService asynchronously #217 
* Support easy configuring of header session mapper #212 
* Smart initialize Job instance #211 
* Deprecate `@Env.Mode` for `@Env.RequireMode` #207 
* Deprecate `@Env.Profile` for `@Env.RequireProfile` #206 
* Deprecate `@Env.Group` for `@Env.RequireGroup` #205 
* Log the URL with handler error message #192 
* Review and fix the use of `ConcurrentMap` #191 
* upport WebSocket #17 S

1.3.14
* Improve maven build process #372
* Improve logging support #370
* Simplify start API/implementation #369
* Introduce osgl-bootstrap and osgl-ut #368
* It shall not try to instantiate commander class for command implemented on static method #367
* NPE when building param tree #365
* Deadlock on app start  #363
* Binder annotation on bean field doesn't work #362
* Support app defined parameter binder annotation #361
* Improve app version reading support #359
* If no file selected in an upload form, the server side will trigger a 500 server error #357 
* `ProvidesImplicitTemplateVariable` generates bad rythm template source code #354 
* The rest parameters is always `null` after `@DbBind` annotated parameter #353 
* Support API document generation #351 
* Improve `Catch` interceptor API #350 
* Get `DbService` list from `DbServiceManager` by plugin class #273 
* Fix regression issues: #287 and #297

1.3.13
* Add annotation to allow developer specify a handler method's template shall not be cached #347 
* Template not found in `prod` mode #346 

1.3.12
* Support in memory cache of uploaded file when size not exceeds threshold #345 
* Support `*` in integer value configuration #344 
* Drop download upload file support #343 
* Make upload file in memory threshold be configurable #341 
* Upload file get saved twice to filesystem #340 
* Make upload file stored in a hierarchical directory structure #339 
* It shall report 400 Bad Request if required file upload is missing #338 
* StaticResourceGetter.toString method output is confusing #337 
* Act's asset static resource URL shall follow the built-in URL convention #336 
* UploadFileStorageService shall add length attribute into SObject #335 

1.3.11
* Error message could not display correctly #330 
* ACT can't register ebean as default datasource when configuration both ebean and mongo datasource #328 

1.3.10
* com.alibaba.fastjson.JSONException: default constructor not found. class act.app.ActionContext #327 

1.3.9
* Random issue: Cannot instantiate interface org.osgl.inject.ScopeCache$SingletonScope #323 
* CLI: Print out the real exception instead of `InvocationTargetException` #322 
* CLI session exit message issue #321 
* i18n message interpolation shall follow standard message format  #320 
* It shall respond with `400` when `DbBind` cannot find the binding value in the request #319 
* Cannot implement Command handler in MorphiaDao #318 
* `@DbBind` not working with JSON format #317 
* Provide a mechanism to allow plugin listen to app hot reload event #316 
* EventBus call on `SimpleEventListener` shall throw out exceptions #313 
* when using `@Output` on field it shall allow method not to have template #312 
* Make `DbBind` annotation to support fetch all data from database #310 

1.3.8
* Add `templatePath` method into `Mailer.Util` #309 
* java.lang.IllegalStateException: parent context not found #307 
* Allow user to set content through `Mailer.Util` #306 
* Add `attach(...)` methods into `Mailer.Util` #305 
* Support early binding of `ActEventListener` #304 
* Add `classForName` method to `App` instance #303 
* `DbBind` comment error #302 
* Allow `DbBind` to use different names to map between request parameter and model field #301 
* It loads the same `routes.conf` file twice #300 
* Suppress `resource:` directive in route table #299 
* The app cannot boot up when static file routing cannot find dir #298 
* JSON binding doesn't work well with @DbBind annotation #297 
* Mail: Sending attachment caused `javax.mail.messagingexception: unknown encoding: utf-8` #294 
* English label is not correct in Act CLI #290 
* Make `@ProvidesImplicitTemplateVariable` support default value #288 
* `@ProvidesImplicitTemplateVariable`: Generic type lost #287 
* When handler has no return value it shall still check the context render arguments #286 
* `@Output` annotation on field declaration does not work #285 
* Add an annotation to support output all controller method parameters into render argument list #284 

1.3.7
* update osgl-mvc to 1.2.0 #276 
* `@AnnotatedWith` injection not work #275 
* `AdaptiveRecord.Util.asMap` method error #272 
* It shall report `405 Method Not Allowed` for HTTP method not recongized #269 
* update fastjson to 1.2.34 #265 

1.3.6
* No log for block issue encountered before loading dependency manager #261 
* Issue with `@DisableFastJsonCircularReferenceDetect` and `@GetAction` #260 
* Improve error message when template not found #258 
* SimpleBean Bytecode scanner issue: interfaces not populated in certain case #254 
* SimpleBean implementation shall be enhanced even without public fields #253 
* download stalled #252 
* StackOverflowError when the class that needs to output in CLI command contains `java.util.Locale` typed field #251 

1.3.5
* Returning Locale type result does not rendering valid JSON response #246 
* Resource consumption issue with DEV mode #244 

1.3.4
* `@Output` annotation on controller field is not effective when handler method has no parameters #202 
* Make mailer support `@TemplateContext` annotation #203 
* App bytecode enhancer state not cleaned #214 
* Improve handling of fatal error in Job method during app bootstrap #216 
* async job is not really async #222 
* double decode of URL path variable #229 
* CLI cannot input negative number #230 
* `RenderAny` shall favor `ActionContext.hasTemplate()` result  #231 
* When action handler returning an object, it failed to apply the `@ResponseStatus` annotation in certain cases #233 

1.3.3
* It does not put correct content type header when servicing static resource as css file bug fixed #200 
* Error generating error page if `Request.accept()` format is not normal #199 
* `@TemplateUrl` annotation on interceptor class shall not impact the template context of handler action #197 
* page cache key is the same for two action handler methods with the same name in different class #196 
* `MorphiaAdaptiveRecord.putValues(Map<String, Object>)` failure #193 
* Allow page cache key generator create different key by checking useragent for mobile/browser #188 
* NPE triggered on actframework official website #187 
* Add Access-Control-Allow-Credentials in CORS support #186 

1.3.2
* functional test cases break when action handler returns array of elements #194 

1.3.1
* It shall not report server error if no file uploaded #189 
* java.lang.NoClassDefFoundError: javax/persistence/Persistence #190 

1.3.0
* Create a mechanism to cache the GET request result #128 
* Introduce `@TemplateContext` annotation #163 
* Split `@Controller` annotation into `@UrlContext` and `@Port` annotation #164 
* `@Global` doesn't work when put behind the interceptor annotation #167 
* Make all scanner favor the setting of `@Env` annotations #168 
* Regex in route not working #169 
* Make it easy to create global template variable #170 
* Add helper javascript library that extends jQuery #171 
* Support profile specific route configuration #174 
* Create better error message when there are error enhancing classes #175 
* Better error reporting when multiple controller action/interceptor methods have the same name #177 
* When handler returns a primitive type the result is not JSON result when `Accept` header require JSON #178 
* Provide an annotation to mark a field or parameter as template variable #179 
* Setting character encoding in response doesn't effect correctly #180 
* Make redirect favor Controller URL context #181 
* Make app able to run `prod` mode from within IDE #182 

1.2.0
* Add an annotation that indicate an injected field is stateless #161 
* Make `ActionContext` an injectable field in `Controller.Util` #160 
* generated pid file not get deleted when app process is killed #159 
* SEO support on routing #157 
* Compile error is not displayed at dev mode #156 
* When `@NotNull` used along with `@DbBind` it shall return 404 if binding failed #153 
* Allow annotation based interceptor class to be registered as global interceptor #152 
* Allow `@With` annotation to be used on specific handler method #136 
* Improve error reporting on "Unknown accept content type" #124 

1.1.2
* Update version of osgl and other dependencies #151 
* Deadlock while app boot up #150 

1.1.1
* Support get process ID on non-unix environment #148 
* Unnecessary synchronization ReflectedHandlerInvoker.checkTemplate #147 
* When db plugin is configured, it uses empty string as service ID #146 
* `EventBus.bind(Object, SimpleEventListener)` shall check if the object type is `EventObject` #144 

1.1.0
* Always generate pid file when app start in prod mode #142 
* Support context URL path #141 
* Cannot use multiple Job annotations on one job method #140 
* allow SimpleEventHandler to be used to handle event happening before app started #139 
* Update FastJson to 1.2.31 #138 
* Provides SqlDbService as a base class for all SQL based DbService solution #135 
* upgrade FastJson to 1.2.31

1.0.7
* ake it able to configure the number of network io threads and work threads #70 M
* configuration render.json.output_charset.enabled default value shall be false #120 
* qrcode method problem #127 
* Response outputstream not closed #130 
* ZXingResult call applyAfterCommitHandler twice #131 
* "type not recognized: MODEL_TYPE" Error when using a DaoBase subclass as Controller #132 
* It uses undertow deprecated API to construct HttpOpenListener #133 
* Fine tune undertow configurations #134 

1.0.6
* caused issue that failed to add route mapping in certain case #121 #115  
* Update fastjson to 1.2.30

1.0.5
* * remove version range from pom.xml. See https://issues.apache.org/jira/browse/MNG-3092

1.0.4
* It shall display the exception stack trace tab on template exception page #109 
* Using simplified action path in @fullUrl and @url doesn't work in an free template #110 
* Routing failure on `/{path1}/{path2}/{path3}/{id}.html` style URL path #111 
* Missing embedded object content when PropertySpec is specified #112 
* Exception encountered when first field of post JSON body contains the parameter name #113 
* Router: support inner variables inside URL path #115 
* Update RythmEngine to 1.2.0
* Update joda-time to 2.9.9

1.0.3
* Error enhancing render arguments when break the statement into multiple lines #68  
* @fullUrl and @url tag doesn't work when there is no GET request mapping to the action handler method #84  
* session.ttl setting prevent app from start up #89  
* Invalid encoded characters in Error page #94  
* Act controller not return correct @version "v" for save method when MorphiaDao return the value bug #97  
* Update FastJson version to 1.2.29 #99  
* when the browser get a json request, Chinese characters are not displayed properly #100 
* IE doesn't support "application/json" content type #101 
* Incorrectly configured routes should not crash hot-reload #104 
* Reloading View manager might break the hot reload process #106 
* Simplify the use of reverse routing API #107 
* Simplify the use of `@url` and `@fullUrl` tag #108 

1.0.2
* It shall allow `null` value for enum type parameter when do the request parameter binding #86 
* DependencyInjectionListener shall register with sub classes of the target class also #87 
* Controller context break with intermediate non-controller class in the hierarchies #88 

1.0.1
* static action handler method cause NPE #79 
* Duplicate route mapping breaks the hot reloading and application state #81 #81 

1.0.0
* First formal release

0.7.0
* Validation refactory

0.6.0
* DAO API change: save(Iterable) now returns list of object been saved

0.5.0
* 0.4.0 reserved for TechEmpower benchmark set
* update dependency versions
* A lot of fix to Adaptive Record
* Dependency Injection improvement on auto binding
* Job parameter binding improvement

0.4.0
* Performance tuning: enable direct io thread processing handler

0.3.1
* ActiveRecord -> AdaptiveRecord
* Performance tuning: enable nonblocking IO

0.3.0
* Catch up update to osgl-mvc 0.6.0: Bind annotation now support specifying multiple Binder implementations

0.2.0
* Make act be java 1.6 compatible
* Big refactoring on
 * dependency injection now on Genie
 * param loading mechanism
 * render arg enhancement now support method call with params, and field

0.1.3
* testapp to implement integration test of ActFramework

0.1.2
* misc bug fixes

0.1.1
* baseline version
