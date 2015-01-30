package org.osgl.oms;

import org.osgl.mvc.annotation.*;
import org.osgl.util.C;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Created by luog on 29/01/2015.
 */
public class OMS {

    private static List<Class<? extends Annotation>> actionAnnotationTypes = C.list(
            Action.class, GetAction.class, PostAction.class,
            PutAction.class, DeleteAction.class);

    public static boolean isActionAnnotation(Class<?> type) {
        return actionAnnotationTypes.contains(type);
    }

}
