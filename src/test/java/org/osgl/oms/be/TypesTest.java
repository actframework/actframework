package org.osgl.oms.be;

import org.junit.Test;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.TestBase;
import org.osgl.oms.asm.Type;

import java.lang.reflect.Method;

public class TypesTest extends TestBase {

    @Test
    public void testMethodDescWithoutReturnType() throws Exception {
        Method m = TypesTest.class.getDeclaredMethod("testMethodDescWithoutReturnType");
        Type mt = Type.getType(m);
        eq(mt.getDescriptor(), Types.methodDesc(Void.class));
    }

    @Test
    public void testMethodDescWithParamAndReturnType() throws Exception {
        Method m = AppContext.class.getDeclaredMethod("param", String.class);
        Type mt = Type.getType(m);
        eq(mt.getDescriptor(), Types.methodDesc(String.class, String.class));
    }

}
