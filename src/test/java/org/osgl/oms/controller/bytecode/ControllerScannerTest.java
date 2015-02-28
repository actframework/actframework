package org.osgl.oms.controller.bytecode;

import org.junit.Before;
import org.junit.Test;
import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.oms.TestBase;
import org.osgl.oms.asm.Type;
import org.osgl.oms.controller.meta.*;
import org.osgl.oms.controller.meta.AppContextInjection.FieldAppContextInjection;
import org.osgl.util.E;
import org.osgl.util.S;
import testapp.controller.WithAppContext;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.osgl.http.H.Method.*;

public class ControllerScannerTest extends TestBase {

    private ControllerClassMetaInfoManager ctrlInfo;

    @Before
    public void setup() {
        super.setup();
        ctrlInfo = new ControllerClassMetaInfoManager(new _.Factory<ControllerScanner>(){
            @Override
            public ControllerScanner create() {
                return new ControllerScanner(mockRouter, new _.F1<String, byte[]>() {
                    @Override
                    public byte[] apply(String s) throws NotAppliedException, _.Break {
                        return loadBytecode(s);
                    }
                });
            }
        });
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
        System.out.println(action);
        assertNotNull(action);
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

    private void scan(Class<?> c) {
        ctrlInfo.scanForControllerMetaInfo(c.getName());
        ctrlInfo.mergeActionMetaInfo();
    }

    private void verifyRouting(String url, String controller, String action, H.Method... methods) {
        for (H.Method method : methods) {
            verify(mockRouter).addMappingIfNotMapped(method, url, "testapp.controller." + controller + "." + action);
        }
    }

    private ControllerClassMetaInfo controller(String className) {
        return ctrlInfo.getControllerMetaInfo("testapp.controller." + className);
    }

    private ActionMethodMetaInfo action(String controller, String action) {
        return controller(controller).action(action);
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

    private void assertFieldAppCtxInject(ActionMethodMetaInfoBase action, String fieldName) {
        FieldAppContextInjection appContextInjection = _.cast(action.appContextInjection());
        eq(fieldName, appContextInjection.fieldName());
    }

    private void assertParamAppCtxInject(ActionMethodMetaInfoBase action, int index) {
        AppContextInjection.ParamAppContextInjection appContextInjection = _.cast(action.appContextInjection());
        same(index, appContextInjection.paramIndex());
    }

    private void assertLocalAppCtxInject(ActionMethodMetaInfoBase action) {
        same(AppContextInjection.InjectType.LOCAL, action.appContextInjection().injectVia());
    }

    private void assertNoParam(ActionMethodMetaInfoBase action) {
        same(0, action.paramCount());
    }
}
