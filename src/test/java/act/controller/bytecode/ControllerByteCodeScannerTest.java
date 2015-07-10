package act.controller.bytecode;

import act.TestBase;
import act.app.AppByteCodeScanner;
import act.app.AppCodeScannerManager;
import act.app.TestingAppClassLoader;
import act.asm.Type;
import act.controller.meta.*;
import act.job.AppJobManager;
import act.job.meta.JobByteCodeScanner;
import act.util.Files;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.Times;
import org.osgl._;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;
import playground.EmailBinder;
import testapp.controller.*;
import testapp.model.ModelController;
import testapp.model.ModelControllerWithAnnotation;

import java.io.File;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.osgl.http.H.Method.*;

public class ControllerByteCodeScannerTest extends TestBase {

    private ControllerClassMetaInfoManager infoSrc;
    private TestingAppClassLoader classLoader;
    private AppCodeScannerManager scannerManager;
    private AppJobManager jobManager;
    private ControllerByteCodeScanner controllerScanner;
    private JobByteCodeScanner jobScanner;
    private File base;

    @Before
    public void setup() throws Exception {
        super.setup();
        controllerScanner = new ControllerByteCodeScanner();
        jobScanner = new JobByteCodeScanner();
        scannerManager = mock(AppCodeScannerManager.class);
        jobManager = mock(AppJobManager.class);
        classLoader = new TestingAppClassLoader(mockApp);
        when(mockApp.classLoader()).thenReturn(classLoader);
        infoSrc = classLoader.controllerClassMetaInfoManager();
        when(mockApp.classLoader()).thenReturn(classLoader);
        when(mockApp.scannerManager()).thenReturn(scannerManager);
        when(mockApp.jobManager()).thenReturn(jobManager);
        when(mockAppConfig.possibleControllerClass(anyString())).thenReturn(true);
        when(mockRouter.isActionMethod(anyString(), anyString())).thenReturn(false);
        C.List<AppByteCodeScanner> scanners = _.cast(C.listOf(controllerScanner, jobScanner));
        //C.List<AppByteCodeScanner> scanners = C.list(controllerScanner);
        when(scannerManager.byteCodeScanners()).thenReturn(scanners);
        controllerScanner.setApp(mockApp);
        jobScanner.setApp(mockApp);
        base = new File("./target/test-classes");
    }

    @Test
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
    public void controllerContextPathShallBeAppendToActionPath() {
        scan(WithContextPath.class);
        verify(mockRouter).addMappingIfNotMapped(GET, "/foo/bar", "testapp.controller.WithContextPath.bar");
    }

    public void verifyWithAppContextNoReturnNoParam() {
        String url = "/no_ret_no_param";
        verifyRouting(url, "WithAppContext", "noReturnNoParam", GET, PUT);

        ActionMethodMetaInfo action = action("WithAppContext", "noReturnNoParam");
        System.out.println(action);

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

    public void verifyWithAppContextStaticNoReturnNoParam() {
        String url = "/static_no_ret_no_param";
        verifyRouting(url, "WithAppContext", "staticReturnStringNoParam", GET);
        ActionMethodMetaInfo action = action("WithAppContext", "staticReturnStringNoParam");

        // verify app context injection
        AppContextInjection appContextInjection = _.cast(action.appContextInjection());
        same(AppContextInjection.InjectType.LOCAL, appContextInjection.injectVia());

        // verify return type
        eq(Type.getType(String.class), action.returnType());

        // verify params
        same(0, action.paramCount());
    }

    @Test
    public void testWithAppContextController() throws Exception {
        scan(WithAppContext.class);
        verifyWithAppContextNoReturnNoParam();
        verifyWithAppContextStaticNoReturnNoParam();
    }

    @Test
    public void testInheritedInterceptor() throws Exception {
        scan(ControllerWithInheritedInterceptor.class);
        assertNotNull(infoSrc.controllerMetaInfo(FilterA.class.getName()));
        assertNotNull(infoSrc.controllerMetaInfo(FilterB.class.getName()));
        ControllerClassMetaInfo info = infoSrc.controllerMetaInfo(ControllerWithInheritedInterceptor.class.getName());
        assertHasInterceptor("FilterA", "afterP10", info.afterInterceptors());
    }

    @Test
    public void testParamAnnotations() {
        scan(ParamWithAnnotationController.class);
        ControllerClassMetaInfo info = infoSrc.controllerMetaInfo(ParamWithAnnotationController.class.getName());
        assertNotNull(info);
        ActionMethodMetaInfo bindNameChanged = info.action("bindNameChanged");
        assertNotNull(bindNameChanged);
        ParamMetaInfo param = bindNameChanged.param(0);
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
        eq(EmailBinder.class, param.bindAnnoInfo().binder(mockAppContext).getClass());
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
        infoSrc.mergeActionMetaInfo();
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
            verify(mockRouter).addMappingIfNotMapped(method, url, "testapp.controller." + controller + "." + action);
        }
    }

    private void verifyNoRouting(String url, String controller, String action, H.Method... methods) {
        for (H.Method method : methods) {
            verify(mockRouter, new Times(0)).addMappingIfNotMapped(method, url, "testapp.controller." + controller + "." + action);
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
                return interceptor(action.beforeList(), className, methodName);
            case AFTER:
                return interceptor(action.afterList(), className, methodName);
            case CATCH:
                return interceptor(action.catchList(), className, methodName);
            case FINALLY:
                return interceptor(action.finallyList(), className, methodName);
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
        AppContextInjection.FieldAppContextInjection appContextInjection = _.cast(action.appContextInjection());
        eq(fieldName, appContextInjection.fieldName());
    }

    private void assertParamAppCtxInject(HandlerMethodMetaInfo action, int index) {
        AppContextInjection.ParamAppContextInjection appContextInjection = _.cast(action.appContextInjection());
        same(index, appContextInjection.paramIndex());
    }

    private void assertLocalAppCtxInject(HandlerMethodMetaInfo action) {
        same(AppContextInjection.InjectType.LOCAL, action.appContextInjection().injectVia());
    }

    private void assertNoParam(HandlerMethodMetaInfo action) {
        same(0, action.paramCount());
    }

    private static enum _F {
        ;
        static _.Predicate<String> SYS_CLASS_NAME = new _.Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.startsWith("java") || s.startsWith("org.osgl.");
            }
        };
        static _.Predicate<String> SAFE_CLASS = S.F.endsWith(".class").and(SYS_CLASS_NAME.negate());
    }
}
