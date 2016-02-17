package act.controller.bytecode;

import org.osgl.http.H;
import org.osgl.mvc.annotation.*;
import org.osgl.util.C;

import java.util.Map;

class AnnotationMethodLookup {
    private static final Map<Class<? extends Action>, H.Method> METHOD_LOOKUP = C.newMap(
            GetAction.class, H.Method.GET,
            PostAction.class, H.Method.POST,
            PutAction.class, H.Method.PUT,
            DeleteAction.class, H.Method.DELETE
    );

    static H.Method get(Class annotationClass) {
        return METHOD_LOOKUP.get(annotationClass);
    }
}
