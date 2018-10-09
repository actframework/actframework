package act.controller.bytecode;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static act.route.RouteSource.ACTION_ANNOTATION;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.osgl.http.H.Method.*;

import act.ActTestBase;
import act.app.*;
import act.asm.Type;
import act.controller.meta.*;
import act.event.EventBus;
import act.inject.param.ParamValueLoaderManager;
import act.job.JobManager;
import act.job.bytecode.JobByteCodeScanner;
import act.route.RouteSource;
import act.util.ClassInfoRepository;
import act.util.Files;
import org.junit.*;
import org.mockito.internal.verification.Times;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.util.*;
import playground.EmailBinder;
import testapp.controller.*;
import testapp.model.ModelController;
import testapp.model.ModelControllerWithAnnotation;

import java.io.File;
import java.util.List;

public class ControllerByteCodeScannerTest extends ActTestBase {

    private ControllerClassMetaInfoManager infoSrc;
    private ClassInfoRepository classInfoRepository;
    private TestingAppClassLoader classLoader;
    private AppCodeScannerManager scannerManager;
    private JobManager jobManager;
    private ControllerByteCodeScanner controllerScanner;
    private JobByteCodeScanner jobScanner;
    private EventBus eventBus;
    private File base;
    private ParamValueLoaderManager paramValueLoaderManager;

    @Before
    public void setup() throws Exception {
        super.setup();
        controllerScanner = new ControllerByteCodeScanner();
        jobScanner = new JobByteCodeScanner();
        scannerManager = mock(AppCodeScannerManager.class);
        classInfoRepository = mock(ClassInfoRepository.class);
        eventBus = mock(EventBus.class);
        when(mockApp.eventBus()).thenReturn(eventBus);
        jobManager = new JobManager(mockApp);
        classLoader = new TestingAppClassLoader(mockApp);
        $.setProperty(classLoader, classInfoRepository, "classInfoRepository");
        when(mockApp.classLoader()).thenReturn(classLoader);
        infoSrc = classLoader.controllerClassMetaInfoManager();
        when(mockApp.classLoader()).thenReturn(classLoader);
        when(mockApp.scannerManager()).thenReturn(scannerManager);
        when(mockApp.jobManager()).thenReturn(jobManager);
        when(mockAppConfig.possibleControllerClass(anyString())).thenReturn(true);
        when(mockRouter.isActionMethod(anyString(), anyString())).thenReturn(false);
        C.List<AppByteCodeScanner> scanners = $.cast(C.listOf(controllerScanner, jobScanner));
        //C.List<AppByteCodeScanner> scanners = C.list(controllerScanner);
        when(scannerManager.byteCodeScanners()).thenReturn(scanners);
        paramValueLoaderManager = mock(ParamValueLoaderManager.class);
        when(mockApp.service(ParamValueLoaderManager.class)).thenReturn(paramValueLoaderManager);
        controllerScanner.setApp(mockApp);
        jobScanner.setApp(mockApp);
        base = new File("./target/test-classes");
    }

    @Test
    @Ignore
    // TODO: route registration is now moved to a job, need new test case for that
    public void specificHttpMethodAnnotationShallNotRegisterOtherHttpMethodsInRouteTable() {
        scan(WithAppContext.class);
        String url = "/static_no_ret_no_param";
        verifyRouting(url, "WithAppContext", "staticReturnStringNoParam", GET);
        verifyNoRouting(url, "WithAppContext", "staticReturnStringNoParam", PUT, POST, DELETE);
    }

    @Test
    public void testNotRoutedActionScan() {
        scan(WithAppContext.class);
        ActionMethodMetaInfo action = action("WithAppContext", "foo");
        assertNull(action);
    }

    @Test
    public void testRoutedActionScan() {
        when(mockRouter.isActionMethod("testapp.controller.WithAppContext", "foo")).thenReturn(true);
        when(mockApp.isRoutedActionMethod("testapp.controller.WithAppContext", "foo")).thenReturn(true);
        scan(WithAppContext.class);
        ActionMethodMetaInfo action = action("WithAppContext", "foo");
        assertNotNull(action);
    }

    @Test
    public void testControllerNotInControllerPackage() {
        scan(ModelController.class);
        ActionMethodMetaInfo action = action("ModelController", "handle");
        assertNull(action);
    }

    @Test
    public void testControllerNotInControllerPackageWithAnnotation() {
        scan(ModelControllerWithAnnotation.class);
        ActionMethodMetaInfo action = action(ModelControllerWithAnnotation.class, "handle");
        assertNotNull(action);
    }

    @Test
    @Ignore
    // TODO: route registration is no moved to a job, need new test case for that
    public void controllerContextPathShallBeAppendToActionPath() {
        scan(WithContextPath.class);
        verify(mockRouter).addMapping(GET, "/foo/bar", "testapp.controller.WithContextPath.bar", RouteSource.ACTION_ANNOTATION);
    }

    @Test
    @Ignore
    public void verifyWithAppContextNoReturnNoParam() {
        String url = "/no_ret_no_param";
        verifyRouting(url, "WithAppContext", "noReturnNoParam", GET, PUT);

        ActionMethodMetaInfo action = action("WithAppContext", "noReturnNoParam");

        // verify app context injection
        assertFieldAppCtxInject(action, "ctx");

        // verify return type
        eq(Type.VOID_TYPE, action.returnType());

        // verify params
        assertNoParam(action);

        // verify interceptors
        InterceptorMethodMetaInfo setup = interceptor(action, InterceptorType.BEFORE, "WithAppContext", "setup");
        assertFieldAppCtxInject(setup, "ctx");
        assertNoParam(setup);

        InterceptorMethodMetaInfo ff_f1 = interceptor(action, InterceptorType.FINALLY, "FilterF", "f1");
        assertLocalAppCtxInject(ff_f1);
        assertNoParam(ff_f1);
    }

    @Test
    @Ignore
    public void verifyWithAppContextStaticNoReturnNoParam() {
        String url = "/static_no_ret_no_param";
        verifyRouting(url, "WithAppContext", "staticReturnStringNoParam", GET);
        ActionMethodMetaInfo action = action("WithAppContext", "staticReturnStringNoParam");

        // verify app context injection
        ActContextInjection actContextInjection = $.cast(action.appContextInjection());
        same(ActContextInjection.InjectType.LOCAL, actContextInjection.injectVia());

        // verify return type
        eq(Type.getType(String.class), action.returnType());

        // verify params
        same(0, action.paramCount());
    }

    @Test
    @Ignore // moved to testapp
    public void testInheritedInterceptor() throws Exception {
        scan(ControllerWithInheritedInterceptor.class);

        assertNotNull(infoSrc.controllerMetaInfo(FilterA.class.getName()));
        assertNotNull(infoSrc.controllerMetaInfo(FilterB.class.getName()));
        assertNotNull(infoSrc.controllerMetaInfo(FilterAB.class.getName()));

        ControllerClassMetaInfo info = infoSrc.controllerMetaInfo(ControllerWithInheritedInterceptor.class.getName());
        assertHasInterceptor("ControllerWithInheritedInterceptor", "afterP10", info.afterInterceptors());
    }

    @Test
    public void testParamAnnotations() {
        scan(ParamWithAnnotationController.class);
        ControllerClassMetaInfo info = infoSrc.controllerMetaInfo(ParamWithAnnotationController.class.getName());
        assertNotNull(info);
        ActionMethodMetaInfo bindNameChanged = info.action("bindNameChanged");
        assertNotNull(bindNameChanged);
        HandlerParamMetaInfo param = bindNameChanged.param(0);
        assertNotNull(param);
        eq("bar", param.bindName());

        ActionMethodMetaInfo defValPresented = info.action("defValPresented");
        assertNotNull(defValPresented);
        param = defValPresented.param(0);
        assertNotNull(param);
        eq(5, param.defVal(Integer.class));

        ActionMethodMetaInfo binderRequired = info.action("binderRequired");
        assertNotNull(binderRequired);
        param = binderRequired.param(0);
        assertNotNull(param);
        assertNotNull(param.bindAnnoInfo());
        eq(EmailBinder.class, param.bindAnnoInfo().binder(mockApp).get(0).getClass());
    }

    @Test
    public void testHelloWorldApp() {
        scan(HelloWorldApp.class);
        verifyNoRouting("/hello", "testapp.controller.HelloWorldApp", "sayHello", H.Method.GET);
    }

    private void scan(Class<?> c) {
        List<File> files = Files.filter(base, _F.SAFE_CLASS);
        for (File file : files) {
            classLoader.preloadClassFile(base, file);
        }
        classLoader.scan();
        classLoader.controllerClassMetaInfoManager().buildControllerHierarchies();
        infoSrc.mergeActionMetaInfo(mockApp);
    }

    private void assertHasInterceptor(String className, String actionName, List<InterceptorMethodMetaInfo> list) {
        if (!className.contains(".")) {
            className = "testapp.controller." + className;
        }
        for (InterceptorMethodMetaInfo info : list) {
            if (S.eq(info.name(), actionName)) {
                ControllerClassMetaInfo cinfo = info.classInfo();
                if (S.eq(cinfo.className(), className)) {
                    return;
                }
            }
        }
        fail("The list does not contains the interceptor: %s.%s", className, actionName);
    }

    private void verifyRouting(String url, String controller, String action, H.Method... methods) {
        for (H.Method method : methods) {
            verify(mockRouter).addMapping(method, url, "testapp.controller." + controller + "." + action, ACTION_ANNOTATION);
        }
    }

    private void verifyNoRouting(String url, String controller, String action, H.Method... methods) {
        for (H.Method method : methods) {
            verify(mockRouter, new Times(0)).addMapping(method, url, "testapp.controller." + controller + "." + action, ACTION_ANNOTATION);
        }
    }

    private ControllerClassMetaInfo controller(String className) {
        return infoSrc.controllerMetaInfo("testapp.controller." + className);
    }

    private ControllerClassMetaInfo controller(Class<?> c) {
        return infoSrc.controllerMetaInfo(c.getName());
    }

    private ActionMethodMetaInfo action(String controller, String action) {
        ControllerClassMetaInfo cinfo = controller(controller);
        return null == cinfo ? null : cinfo.action(action);
    }

    private ActionMethodMetaInfo action(Class<?> c, String action) {
        ControllerClassMetaInfo cinfo = controller(c);
        return null == cinfo ? null : cinfo.action(action);
    }

    private InterceptorMethodMetaInfo interceptor(ActionMethodMetaInfo action, InterceptorType interceptorType, String className, String methodName) {
        switch (interceptorType) {
            case BEFORE:
                return interceptor(action.beforeInterceptors(), className, methodName);
            case AFTER:
                return interceptor(action.afterInterceptors(), className, methodName);
            case CATCH:
                return interceptor(action.exceptionInterceptors(), className, methodName);
            case FINALLY:
                return interceptor(action.finallyInterceptors(), className, methodName);
            default:
                throw E.unexpected("unknown interceptor type: %s", interceptorType);
        }
    }

    private InterceptorMethodMetaInfo interceptor(List<? extends InterceptorMethodMetaInfo> list, String className, String methodName) {
        String fullName = S.join(".", "testapp.controller", className, methodName);
        for (InterceptorMethodMetaInfo info : list) {
            if (S.eq(fullName, info.fullName())) {
                return info;
            }
        }
        return null;
    }

    private void assertFieldAppCtxInject(HandlerMethodMetaInfo action, String fieldName) {
        ActContextInjection.FieldActContextInjection appContextInjection = $.cast(action.appContextInjection());
        eq(fieldName, appContextInjection.fieldName());
    }

    private void assertParamAppCtxInject(HandlerMethodMetaInfo action, int index) {
        ActContextInjection.ParamAppContextInjection appContextInjection = $.cast(action.appContextInjection());
        same(index, appContextInjection.paramIndex());
    }

    private void assertLocalAppCtxInject(HandlerMethodMetaInfo action) {
        same(ActContextInjection.InjectType.LOCAL, action.appContextInjection().injectVia());
    }

    private void assertNoParam(HandlerMethodMetaInfo action) {
        same(0, action.paramCount());
    }

    private enum _F {
        ;
        static $.Predicate<String> SYS_CLASS_NAME = new $.Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.startsWith("java") || s.startsWith("org.osgl.");
            }
        };
        static $.Predicate<String> SAFE_CLASS = S.F.endsWith(".class").and(SYS_CLASS_NAME.negate());
    }
}
