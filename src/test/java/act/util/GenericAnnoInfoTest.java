package act.util;

import act.TestBase;
import act.asm.Type;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.junit.Before;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Proxy;

public class GenericAnnoInfoTest extends TestBase {
    private GenericAnnoInfo actionInfo;
    private GenericAnnoInfo notNullInfo;

    @Before
    public void setup() {
        actionInfo = new GenericAnnoInfo(Type.getType(Action.class));
        actionInfo.putListAttribute("value", "/foo");
        actionInfo.putListAttribute("methods", H.Method.GET);
        actionInfo.putListAttribute("methods", H.Method.POST);

        notNullInfo = new GenericAnnoInfo(Type.getType(NotNull.class));
        notNullInfo.putListAttribute("group", Action.class);
    }

    @Test
    public void annotationProxyTest() {
        Action action =  actionInfo.toAnnotation();
        eq(action.value(), new String[]{"/foo"});
        eq(action.methods(), new H.Method[]{H.Method.GET, H.Method.POST});

        NotNull notNull = notNullInfo.toAnnotation();
        eq(notNull.message(), "{javax.validation.constraints.NotNull.message}");
    }

}
