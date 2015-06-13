package act.util;

import act.TestBase;
import act.asm.Type;
import org.junit.Before;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;

public class GenericAnnoInfoTest extends TestBase {
    private GenericAnnoInfo actionInfo;

    @Before
    public void setup() {
        actionInfo = new GenericAnnoInfo(Type.getType(Action.class));
        actionInfo.putListAttribute("value", "/foo");
        actionInfo.putListAttribute("methods", H.Method.GET);
        actionInfo.putListAttribute("methods", H.Method.POST);
    }

    @Test
    public void annotationProxyTest() {
        Action action =  actionInfo.toAnnotation();
        eq(action.value(), new String[]{"/foo"});
        eq(action.methods(), new H.Method[]{H.Method.GET, H.Method.POST});
    }
}
