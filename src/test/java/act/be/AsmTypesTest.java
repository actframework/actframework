package act.be;

import org.junit.Test;
import act.app.ActionContext;
import act.TestBase;
import act.asm.Type;
import act.util.AsmTypes;

import java.lang.reflect.Method;

public class AsmTypesTest extends TestBase {

    @Test
    public void testMethodDescWithoutReturnType() throws Exception {
        Method m = AsmTypesTest.class.getDeclaredMethod("testMethodDescWithoutReturnType");
        Type mt = Type.getType(m);
        eq(mt.getDescriptor(), AsmTypes.methodDesc(Void.class));
    }

    @Test
    public void testMethodDescWithParamAndReturnType() throws Exception {
        Method m = ActionContext.class.getDeclaredMethod("paramVal", String.class);
        Type mt = Type.getType(m);
        eq(mt.getDescriptor(), AsmTypes.methodDesc(String.class, String.class));
    }

}
