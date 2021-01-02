# ActFramework Change Log

**1.9.1** 02/Jan/2021
* Shutdown app gracefully in Runtime shutdown hook
* Act-Test - support sending request with file array #1375
* It reports `UNKNOWN` for OS when running act on macOS #1373
* Hot reload not working for Bundle Resource properties #1372
* Cannot start ActFramework: port xxxxx is occupied #1370
* ApacheMultipartParser NullPointerException #1369
* EhCache raised `ClassCastException` after reload in dev mode #1368
* act-test: number verification logic error #1361
* Error encountered requesting `/asset/extjs-all.js` #1359
* 500 Error but not error stack in console log #1358
* Support Java 14 Record class #1354
* Allow the developers to specify ECJ compiler options #1353
* `null` file object inject from form field into the request handler argument list #1352
* When it detect error during generating request handler instance it shall mark it as a fatal error #1347
* Support customised resource bundle encoding #1329

**1.9.0a** 28/Jun/2020
* Add `@Inject` to CliDispatcher constructor - allow it be injected in - e.g. - HelpPage
* The error triggered during rendering response get warned twice #1341
* Support running act app in Java9 or above #1342
* SimpleBean bytecode enhancer caused "java.lang.ClassFormatError: Interface cannot have a method named <init>" issue #1344

**1.8.33b** 27/Jun/2020
* Update to osgl-tool 1.25.0
* Missing fields exporting AdaptiveRecords in csv format #1340
* In case Route mapping exception occurred, it shall display the relevant source file and highlight the place where mapping failed #1313
* update fastjson to 1.2.71
* API doc - URL path variable in POST endpoint info is incorrect #1284
* Scenario manager - support loading test scenario files from child folders recursively #1337
* Param value loader framework - allow inject another controller class #1336
* `EnhancedAdaptiveMap.asMap(EnhancedAdaptiveMap)` generated `Map` shall implement hashCode and equals methods #1333
* Session cache shall be cleared after app hot reload #1330 - update osgl-http to 1.13.2
* Improve http cache support #1328
* Add profile to default session cookie name #1326
* It needs to expose CORS control headers to all kind of requests #1325
* Invalid warning msg: "empty index encountered" #1322
* CLI over HTTP - allow specify big string which require textarea as input #1321
* Add helper endpoint in dev mode to get Java System Properties #1320
* FileBinder - When running command from CLI over HTTP, bind to new File #1319
* Allow app to fetch configuration from a configuration server #1318
* skip loading "META-INF/versions/9/module-info" class #1288
* Choose port randomly when running automation test in batch mode #1317
* wrong file object inject from form field into the controller param #1316
* API doc - URL path variable in POST endpoint info is incorrect #1284
* `SampleData.ProvidedBy` is not effective on int type field #1310

**1.8.32** 04/Mar/2020
* ConcurrentModificationException calling eventBus.triggerAsync #1308
* Allow access CLI over HTTP #1305
* CLI help message not displayed correctly #1303
* Act-Test - allow specify collection size when generating List/Set random collection #1301
* Act-Test create sample data provider for `id` field #1300
* When it rendering data in HTML table, the column heading does not follow `PropertySpec` specification #1299
* When requesting data from browser directly render the data using HTML table format if it is an `Iterable` #1298
* Automatically register all CLI command handler as Get request handler to CLI over HTTP port #1297
* Render attachment or inline response when request handler return `File` or `ISObject` type based on its content type #1296
* It respond with 500 error when file not found raised from IStorageService #1295
* Act-Test - it shall send session header token from last headers automatically #1294
* Allow it use request parameter to pass session token #1293
* System boot process crashed when there is problem generating banner image from icon file #1292
* HTML table view - make table header sticky at the top #1290
* API Doc - improve sample data generation for `ISObject` type #1289
* Add jackson into jar black list. Refer: #1288
* async request handling - render report using cached accept type #1287
* Exception handling controller with field class without generic parameter specified #1286
* Async endpoint result page style needs to be updated #1285
* API doc - URL path variable in POST endpoint info is incorrect #1284
* File upload generated file name might contains double `.` result in file cannot be accessed #1282

**1.8.31** 02/Jan/2020
* Act-Test - Add `In` verifier #1271
* Create an API to allow developer generate random data #1270
* Enable turn off QRCode/BarCode rendering based on Accept header #1269
* ParamValueBinder - allow bind to ActionContext renderArgs #1267
* CLI - add shortcut into the command help list #1266
* Disable WARN message on TopLevelDomainList download error when act is running in a non-internet environment #1265
* No response on certain error scenario #1264
* Support JSON path #1262
* html-table rendering issue with a list of String #1261
* IE 9 browser，got a strange error！ #1260
* ApiDoc bug: JPA Entity field #1259
* XML Data bind to JSONObject issue #1258
* API Doc - sample query data issue with array type #1257
* JSON Error message should escape `"` #1256
* Act should log error stack upon `IllegalArgumentException` explicitly and respond 500 status #1254
* `LoadResource` - error injecting into `C.Map` typed field #1253
* Param binding - allow specify `now` in `DefaultValue` for Date types #1247

**1.8.30a** 24/Nov/2019
* Fix issue - the upload file not found after moving testing resources to `src/test/resources`

**1.8.30** 23/Nov/2019
* App failed to start without error stack #1252
* Act-Test - Allow it to load test files from src/test/resources folder #1249
* API Doc - Return sample for array is not correct #1250
* SimpleRestfulServiceBase - property spec enhancements #1248
* API doc - DOB sample data generator generates invalid date string #1246
* Allow it specify particular part of returned data in response using `_jsonPath` parameter #1245
* Support `htmltable` pseudo Accept type #1244
* Ehcache 在 热重载时 无法找到 EhCacheServiceProvider 的 _reset 方法。 #1243
* `__path` param value needs to be sanitized #1241
* Act-Test - add an `entityId()` function #1240

**1.8.29** 03/Nov/2019
* SimpleRestfulServiceBase - allow developer to inject logic #1237
* Allow app to specify sample data provider for any field #1236
* Enable render QRCode using pseudo accept parameter #1235
* Creating a system self healing mechanism to handle OOM caused by too many dangling connention #1234
* Add commonly used cron expression constants #1229
* Ehcache not working in Act since 1.8.27 #1220
* CORS - update default allowed headers #1228
* Allow using query parameter `_accept` to overwrite the `Accept` HTTP header #1227
* SimpleRestfulServiceBase - support filtering/pagingation/sorting #1226
* ResourceLoader - Error loading Yaml data into List of entities #1225
* ApiDoc - missing description for classes extends from `SimpleRestfulServiceBase` #1224
* Markdown response support #1219
* $$.processStringSubstitution` issue #1223
* `@LoadResource` mechanism not doing resource filtering even `resource.filtering` is set to `true` #1222
* `UnexpectedException: type not recognized: ? extends T` - JsonDtoPatch logic issue #1218
* CLI table view layout broken due to multi-bytes characters #1217
* Incorrect XML output for iterable type #1216
* Support output yaml response #1215
* Create built-in CLI session history mechanism #1214
* Allow app to configure CLI progress bar style #1212
* Incorrect message returned by `ActContext.i18n(template, args)` #1211
* Provide a default HTML page for async controller response #1209
* Allow it auto refresh when got 409 error during hot-reloading #1207
* "The response has already been started" error #1208
* Update FastJSON to 1.2.62 #1205

**1.8.28** 21/Sep/2019
* Session resolve issue for concurrent incoming requests #1204

**1.8.27** 15/Sep/2019
* Hotreload error - Provider has already registered for spec: class act.aaa.AAAService #1201
* Act-Test - Improve dependency/fixture management #1198
* Act-Test - dependency order not maintained when setup is true #1195
* ConcurrentModificationException on detecting file changes #1194
* Hotreload logic - it shall save ActionContext to local before checking file changes #1193
* TopLevelDomainList - fix NPE caused when job executing during app hotreload process #1192
* FastJsonJodaDateCodec - allow it to deserialize timestamp (long) data #1191
* Act-Test - add randDouble and randFloat function #1190
* Act-Test - allow run scenarios for specific partition #1189
* Act-Test - display partition name in the title #1187
* Update to osgl-tool-1.20.1 and osgl-cache-1.7.1 #1186
* No need to invoke hotreload listener if app is not running in DEV mode #1185
* Once add ehcache dependency `maven package` process will never end #1184
* Command register - it shall not register a command on abstract class #1183
* CommandBytecodeScanner - it needs to register command by name along with prefix #1182
* ApiDoc - add syntax highlight for JSON samples #1181
* ResourceGetter - accessing folder that access is not allowed cause response never close #1180
* ActionContext not released when `ResourceGetter` is handling request #1179
* ConcurrentModificationException when output to csv with PropertySpec specified #1178
* `FixtureLoader` not effective after called #1177
* Act-Test - NPE encountered when scenario is parsed after scenario depend on it parsed #1176
* NPE triggered in `requestThrottleExpireScale` after hot reload #1175
* Change Content-Type header for JSON error response #1173

**1.8.26** 21/Jul/2019
* update osgl-tool to 1.20.0
* update genie to 1.11.0
* API doc - allow landing to specific part when open link with hash #1171
* Act-Test - when dependent scenario is running in a different partition it shall be run anyway even it has been finished before #1169
* Act-Test download checksum verification might cause NPE #1168
* Act-Test - circular dependency caused by `setup` #1167
* View plugin get initialised twice #1166
* Scenario spec - cannot specify long value for substitution #1164
* LoadConfig - suppress warning messages #1163
* Act-Test - list failed scenario names in the splash page #1162
* Act-Test - fixture clearing issue with multiple level dependencies bug #1161
* When uploaded an empty file, the parameter injected is `null` #1160
* Test Scenario - allow specify response spec without type block #1159
* Test Scenario - allow specify ignore reason #1158
* Keep field definition order when output entity list into Excel file #1157
* Make `AppServicePlugin` extends `LogSupport` #1156
* False alarm caused by render inline template #1155
* False alarm message about local variable table not found on app start #1154

**1.8.25** 2/Jul/2019

* Allow inject DbService #1153
* NPE on creating controller instance #1152

**1.8.24** 22/Jun/2019
* Allows resource filtering in dev mode #1151
* CSV view - support AdaptiveBeanBase typed data #1149
* App halt when there are block issue on startup #1148 
* Resource loader - improve Map loading - support different key val types #1147
* Resource loader - handle comment in files for map loading #1146
* Downloading a big CSV file the second time failed #1145
* CLI - Async progress bar reporting eat up CPU #1144


**1.8.23** 16/May/2019
* CLI Table view - print line interlaced with revert color/bg color #1143
* Add `@Label` annotation for bean field marking #1142
* Email validation handler - not able to valid email at `thinking.studio` #1141
* CLI - session exit immediately #1140
* Improve async CLI command error reporting #1139
* `@Every` annotation not working as expected if `@OnAppStart` annotation presented #1138
* Router - debug routing list missing certain routes #1137

**1.8.22** 20/May/2019
* `route.print` not handle keyword matching routes correctly #1136
* Allow developer to disable JSON body patch #1134
* API Doc - treat session variable as a special param #1133
* API doc - catch field javadoc comment and add it to API doc #1132
* Enhance AdaptiveBean - better support to inheritance #1131
* Allow developer to specify property naming convention for JSON output #1130
* Upload file issue #1128
* Act-Test add synonyms to cache/assign #1126
* Act-Test add `startsIgnoreCase` and `endsIgnoreCase` verifier #1125

**1.8.21** 26/Apr/2019
* Update jQuery to 3.4.0 #1123
* Add String to LocalDate and String to LocalTime type converter #1124

**1.8.20** 20/Apr/2019
* Act-Test - Add alias `assign` to `cache'; improve explicit issueKey handling #1120
* Request handler - error output when ReturnValueAdvice and PropertySpec presented for Iterable type return #1118
* `UnexpectedException` triggered executing CLI command in TSCPP project #1117
* Configurable URL router support #1116
* Act Test - running duplicated scenarios due to introduce of RefId #1115
* Act Test - fixture not cleared in certain case #1114
* Act Test - intelligent scenario name #1113
* Validation message - remove the logic that wrap user defined message with `{` and `}` #1112
* Validation message - allow developer to specify not prepend bean/property path #1111
* Act-Test: Add issueKey to Scenario #1108
* Improve `~/test` page rendering #1105
* Mock data function for development #1106
* Add headerNames() to UndertowRequest #1104
* Add `-parameters` argument to ECJ #1103
* Act-Test add containsIgnoreCase verifier #1102
* Provide mechanism to inspect Job failure #1100
* request handler - allow bind to a path in JSON body #1099
* Exception raised before committing to response shall trigger another 500 response #1097
* OsglConfig.internalCache shall be cleared before DB start init #1098
* java.lang.LinkageError caused by org.w3c.dom.Document #1096
* ActFramework hot reload caused Ehcache Classloader Exception #1070
* `@LoginUser` caused `InjectException` upon hot reload #1071
* DataPropertyRepository - ClassCastException encountered building property path #1095
* DataPropertyRepository - StackOverflowError building property path for model with circular reference #1093
* Error encountered with customised Unique validator when working with JPA #1069
* Act-Test - add refId property to Scenario #1091
* Add switch to disable auto hot reload in DEV mode #1090
* GH 1078 - Log warn message when `@Global` is found on instance method of an abstract class
* API Doc - handle `@inheritDoc` tag #1089
* ReflectedInvokerHelper treated `Set` as stateless type #1088
* ParamValueLoaderService - PARAM_TREE not cleared in certain case #1087
* GH 1072 - improve dao init logic, log warn message when an non-abstract Dao class
  cannot be materialized because of missing type param implementation  
* API Doc - improve support for endpoints defined in parent class #1086
* API doc - make it consistent for styling of javadoc code block and sample code block #1085
* API doc - capture field javadoc comment #1084
* `ISObject` is not null when no file is uploaded #1083
* Upload file caused server crash #1082
* Event bus - simple event key matching logic needs improvement #1077
* Act-Test - print colourised output when run batch test #1081
* Act-Test - allow developer specify a model shall not be cleared at fixture loading #1080
* Error visiting page with IE 8 or below #1079
* Misleading error stack printed out when loading yml file with `@LoadResource` #1073
* Act-Test - JSON request array content get dropped #1076
* Add filter box in API page #1074
* Act-Test - add `ignore` flag to scenario #1075
* JPAContext is not readay error when app start asynchronously #1063
* StackOverflowError caused by JsonDtoPatch #1064
* Multiple improvements on sys admin command #1068
  + simplified command name aliases for system command: xxx.list
    - route.list -> route or routes
    - metric.timer.list -> metric.timer 
                        or metric.timers 
                        or metric
                        or metrics
                        or timer
                        or timers
    - metric.counter.list -> metric.counter
                        or metric.counters
                        or counters
    - job.list -> job or jobs
    - daemon.list - daemon or daemons
    - conf.list
  + method/field access error due to different classloader for XxxAdmin and the access type
  + uniformed `q` parameter handling for list commands 
* Improve metric timer
* envMatches in ControllerByteCodeScanner shall be reset upon scanning new class #1065
* Act-Test - support post XML encoded body #1062
* API book - Module name shall include enclosing class #1059
* API doc - line breaks in param description get removed #1060
* metric command stop working #1058

**1.8.19** 13/Feb/2019
* Update dependency versions
* Test yaml - variable not evaluated when putting in an array #1055
* Allow app to configure automate test http request timeout #1051
* Error enhancing class with `@Data` annotation #1049
* action method parameter cache #1054
* Route information missed in API doc for dynamic aliases #1052
* command `route.print -t` triggers error #1053
* `@WsEndpoint` annotation now failed to register route #1050

**1.8.18** 04/Feb/2019
* Fix JobContext init logic
* Error enhancing bytecode at com.sun.mail.util.logging.MailHandler::publish() #1048
* ContextualJob cause JobContext get cleared twice #1046
* JobContext lost when invoking another job #1044
* Send out Login and Logout event #1043
* Support `Keyword` matching for param binding of incoming request #972
* Allow load route configuration from `route.conf` file #1042
* Allow keep searching route table even when a terminate node found #1041
* act test failed in mvn could not see detail information #1040
* mvn package will throw IllegalStateException #1039

**1.8.17** 23/Dec/2018
* UnexpectedException when submit a `Map<String, T>` type parameter with value as `null` #1027
* Support static apibook generation #893
* Morphia ObjectId not generate in api-book #1033
* Bad behaviour when Error result returned on Accept excel mime type #1034
* Test report page improvement #1032
* Provide a way to handle generic param type loading #1031
* UnexpectedException when submit a `Map<String, T>` type parameter with value as `null` #1027
* Static file handler - the file handle not closed after serviced #1028
* Configure healthy monitor and report server status #1021
* Test - json param shall be processed #1025
* Test - allow it post body for DELETE request #1024
* Annotations with ValueLoader cannot work if post with json #1016
* Routing issue with dynamic aliases and keyword matching #1022
* Make CLI help command default to display application commands #1018
* CLI - execute `conf.trace-handler -e` trigger `IllegalAccessError` #1019

**1.8.16** 09/Dec/2018
* `StackOverflowError` caused by POJO typed request handler method param circular reference #1015
* Allow configure XML root element tag #1011
* Allow load resource from `.xml` file #1012
* `NullPointerException` caused by sending GET request with `Content-Type=application/xml` header #1008
* api-book-compile cannot get comments from super class #1003
* act test failed when check return data exist #1010
* Calling mailer method asynchronously by eventBus.trigger didn't work #1009
* @PropertySpec and PropertySpec.current.set cannot always work when object wrapped by GlobalAdvice #1006
* @PropertySpec and PropertySpec.current.set cannot work when object wrapped by GlobalAdvice #1005
* Make `TimestampAuditor` defined in act-jpa project be common #1002
* Make `CommandPrefix` be inheritable #998
* Request handler argument type with type parameter does not work #1000

**1.8.15** 30/Nov/2018
* Introduce `@CommandPrefix` annotation #982
* `mi` make it display number in MB by default #996
* `IllegalAccessError` triggered while running `mi` command #995
* compile-api-book error when super class has generics #987
* SimpleRestfulServiceBase 'update' enhancement #976
* @After @Finally not effect when @Valid failed #988
* Hot reload in dev mode always fail at the first POST request #989
* Add method to handle connection close event to `WebSocketConnectionListener` #994
* websocket connection not released after closed #991

**1.8.14** 28/Nov/2018
* act command throw IllegalStateException: JPAContext is not ready #973
* java.lang.IllegalStateException: UT000146: HttpServerExchange cannot have both async IO resumed and dispatch() called in the same cycle #974
* Support generating sample data for `org.osgl.util.Keyword` typed field #986
* @Before @After not effect when sub class extends from super class #985
* It shall force marking progress gauge as done once the job is returned #971
* `DaoLoader` not able to load Dao class without type parameter #979
* It shall not set `null` to field marked with `@Configuration` if the setting is not configured #984
* CLI table view does not work properly with `Keyword` typed field #983
* Drop websocket connection subscribed to listen job progress once job is finished #977
* Make the `dao` field of `SimpleRestfulServiceBase` be protected #980
* Push progress only when `ProgressGauge`'s percentage changed #975

**1.8.13** 25/Nov/2018
* Provides a mechanism for accurate routing ws connection event to user defined connection handler #961
* `NullPointerException` when route path element wrapped with `~` is no the final one #958
* act.session.ttl=-1 not effect when JWT is enabled #968
* `App.singleton(Xyz.class)` returns `null` value #967
* `@CsvView` and `@TableView` not effective when mark on request handler method #966
* Keep decorator setting of async command/request #965
* `ProgressGuage.markAsDone` shall not trigger event if progress is already done #963
* Provide a mechanism to handle async process result #960
* `WebsocketConnectionRegistry` - provide method to remove key #962
* Issue with using `WebSocketConnectEvent` to call websocket connection handler #959

**1.8.12** 20/Nov/2018
* Change ws endpoint for job progress status, add GET endpoint for job progress checking #957
* The `/~/job/progress` ws endpoint does not work #956
* If interceptor is disabled then it shall not return `404` #955
* False warning message on DB configuration #953
* CLI session shall not time out when it is reporting async job progress #952
* `NullPointerException` invoking `job.list` command #950
* Allow it to turn on/off handler trace while app is running #949
* execute `route.list` command causes `NullPointerException` #948
* `NullPointerException` when calling `$.convert(str).to(DateTime.class)` in Job #947
* Output enhanced asm code when `java.lang.VerifyError` encountered #945
* `UnexpectedException` with `Controller.Util.download(URL)` call #944
* Deadlock issue during app hot reload #941
* Allow app to do keyword matching on certain route path element #939
* Missing `Content-Type` header in response servicing static file request #937
* Hot-reload issue caused by `StackOverflowError` on `AppConfig.loginUrl()` #936
* Handle `Error` encountered processing incoming request #933

**1.8.11** 5/Nov/2018
* Automate test failure #932
* update dependencies
    - undertow-core: 1.4.26.Final
    - snakeyaml: 1.23
    - reflectasm: 1.11.7
    - joda-time: 2.10.1
    - jline: 2.14.6

**1.8.9** 4/Nov/2018
* update jline to 2.14.4
* CLI - support `@DefaultValue` #929
* Allow app to customise error response upon invalid request #922
* `java.lang.IllegalArgumentException` upon starting BSBF project #931
* Hot reload is broken in R1.8.8 version #921
* CLI - do not output `null` for options without help message #927
* API doc - allow fault tolerant when generating sample data #920
* API doc - it shall support `@Sensitive` annotation #919
* `UnexpectedClassNotFoundException` raised during restoring plugin classes #923
* `Unable to find the overwritten method of Xxx` issue on 1.8.8 #925

**1.8.8** 30/Oct/2018
* Cannot add white space for `session.header.payload.prefix` configuration #918
* `App.getResource(String)` behavior different between dev and prod mode #916
* Make `Dao` by default be stateless #914
* Optimize Singleton controller instance infer logic #913
* `SubClassFinder` and `AnnotatedClassFinder` shall favor `NoAutoRegister` annotation #912
* Test - enhance ${now()} function #911
* Test - allow it add `negative` decorator to verifier #910
* Test - remove generic type parameter from `NamedLogic` #909
* Test - allow setting precision of ${now()} function #908
* Interceptor defined in super class now not effective on sub class #907
* Basic support for xml content-type #905
* ReflectedHandlerInvoker - try to get Annotation from method in parent class if possible #906
* JsonView, CsvView etc shall be declared as `@Inherited` #904
* Test debug page - make failed test display in the beginning of the page #896
* SimpleRestfulServiceBase - further simplified the usage #898
* API sample data - add URL category #897
* `JPAContext` not closed when Error response is generated #895
* Allow app to delay automate testing #894
* Add `removeTag`, `reTag` method to `WebSocketContext` #892
* Stop loading API manager when running in `test` profile or `prod` mode #890
* Caused by: act.app.CompilationException: TestBase cannot be resolved to a type #889
* Failed to instantiate abstract class when building interceptor list in a rare scenario #888
* Route table: support whitespace in URL path #887
* `context.renderArg(key, val)` not cleared in prod mode #886
* make jquery extension library process xsrf token cookie automatically #884
* CSRF - the XSRF token cookie shall not be set as httpOnly #883
* Test - partition scenarios #877
* Add `SysEventId.POST_STARTED` #882
* Support opt-out jars from managed classloader by specifying file name prefix in `act.jar.black.list` #881
* No need to trigger hotreload when testing file changes #880
* @PropertySpec could not effect entity in collection #878
* It randomly found request handler parameters are not enhanced with `@Named` annotation #879
* Test: add $now() and $today() function #876
* Intermittent ACT start failure in `GenieModuleScanner` #875
* Route configured for namedport not effective #874
* `EntityClassMetaInfo.mergeFromMappedSuperClasses` logic error #873
* Optimize app code start in dev mode #872
* Test: evaluation complex expression refer to cached object issue #871
* Act not return application/json as default when exception #870
* Test - Allow app to define url context for scenario #869
* Add jobId to all built-in jobs #868
* API Doc: generate sample data for interface #867
* `PropertySpec` specification not working when return value is a list #866
* Multiple instances issue with request handlers/interceptors happens across class hierarchies #865
* Support `@Order` in intercepting #864
* Support using `SessionVariable` annotation and `DbBind` together #862
* Make `EventBus` and `Job` list favor `Order` annotation #861
* test resource not refreshed after hot-reload #860
* Rythm Template error report - source tab issue #859
* Render automated test report for JSON request #858
* Provide a way to allow app to specify order of elements in the injected collection #857
* `@SessionVariable` annotation not working as expected #856
* Automate testing failure #855
* `@PropertySpec` not effect when `ReturnValueAdvice` applied #852
* Allow it to run specific test scenario #851
* `@PropertySpec` not applied to excel download #848
* Add code and message support for unauthorizedIf function #847
* It takes over 20s to bootstrap app in dev mode with large file in resources dir #846
* Create an annotation to help specify head mapping for `@LoadResource` mechanism #844
* FastJson exception when serialize ACT error message #841
* `CacheFor` - make it allow skip `Cache-Control` header #837
* `ResponseCache` - content disposition shall be cached #838
* `CacheFor` key shall include `Accept` type #836
* Provide a mechanism to allow developer advice on return value of request handler method #835
* Add render image support #834
* Optimise `LogSupport` for `DestroyableBase` #833
* Allow app developer to specify download file name #829
* `ActionContext.allowIgnoreParamNamespace()` overlooked by `MapLoader` #827
* Use `JSON.toJSONString()` to replace `Object.toString()` whenever it is used to render response body #826
* Force response status not working when response type is JSON #825
* Apply new scope for implicit transaction #823
* When returning String is not a valid JSON, it shall be encapsulated with `result` #821
* Using generic typed injection in Controller cause `UnexpectedException` when start up #820
* Extended request handler method's param not enhanced with `@Named` annotation #819
* maven pom: It shall add `.tag` file into resource filtering list #817
* `Controller.renderHtml()` method signature error #816
* `jquery.ext.js` - undefined error checking ajax redirect when there is no content in body #815
* API doc - support module #814
* Cannot render Excel when return value is `Map` type #813
* `java.lang.IllegalStateException`: job already registered: __act_sys__start-delay-1 #812
* Report error when app's package starts with `act` #811
* Rename "e2e" to "test" #810
* Bytecode enhanement error on App start #809
* e2e - support customised fixture loading logic #808
* Environment assert - support multiple values #807
* e2e - allow loading fixtures from JSON file #806
* e2e: Allow suppress fixture clearing #805
* Add `Page` data structure for db list operation #804
* E2E - `NullPointerException` with interdependent scenarios #803
* JSON request resolving - binding String to class issue #802
* APIDoc - make sample email matches sample firstname and lastname #801
* e2e - random model data generation #800
* e2e function: It generated invalid email #799
* `@JSONField(format = "yyyy-MM")` setting not work #798
* JSON output: default format for `java.util.Date` missing time part #797
* `@PropertySpec` annotation now cause `NullPointerException` on JSON output #796
* `@CacheFor` annotation cause incomplete response in PROD mode #794
* Add `renderBinary(byte[])` and `renderBinary(byte[], String)` method to `Controller.Util` #793
* Attach `PropertySpec` info to `ActionContext` #792
* Resource loader - allow loading from excel file #790
* e2e - support email content verification #789
* e2e - support embed function in string for constant definition #788
* Empty `scenario.yml` file caused `NullPointerException` #787
* `@Data` enhancement shall call super by defau lt for Map/AdaptiveMap types #786
* Add `notEqual` e2e verifier #785
* `@Data` enhancement error when Model class has no fields #784
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
* * remove version range from pom.xml. See <https://issues.apache.org/jira/browse/MNG-3092>

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

**0.3.0** 10/Oct/2016
* Catch up update to osgl-mvc 0.6.0: Bind annotation now support specifying multiple Binder implementations

**0.2.0** 20/Sep/2016
* Make act be java 1.6 compatible
* Big refactoring on
 * dependency injection now on Genie
 * param loading mechanism
 * render arg enhancement now support method call with params, and field

**0.1.3** 19/Jun/2016
* testapp to implement integration test of ActFramework

**0.1.2** 11/Apr/2016
* misc bug fixes

**0.1.1** 21/Feb/2016
* baseline version
