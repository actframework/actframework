package org.osgl.oms.controller.meta;

import org.osgl.http.H;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.annotation.*;
import org.osgl.oms.asm.Type;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.annotation.Annotation;

/**
 * Stores all class level information to support generating of
 * {@link org.osgl.oms.controller.RequestDispatcher request dispatcher}
 * and {@link org.osgl.oms.controller.Interceptor interceptors}
 */
public final class ControllerClassMetaInfo {

    private static final Logger logger = L.get(ControllerClassMetaInfo.class);

    private Type type;
    private boolean isAbstract = false;
    private String ctxField = null;
    private C.Set<String> withList = C.newSet();
    private C.List<ActionMethodMetaInfo> actions = C.newList();
    private C.Map<String, ActionMethodMetaInfo> actionLookup = null;
    private GroupInterceptorMetaInfo interceptors = new GroupInterceptorMetaInfo();

    public ControllerClassMetaInfo className(String name) {
        this.type = Type.getObjectType(name);
        return this;
    }

    public String className() {
        return type.getClassName();
    }

    public String internalName() {
        return type.getInternalName();
    }

    public ControllerClassMetaInfo setAbstract() {
        isAbstract = true;
        return this;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public ControllerClassMetaInfo ctxField(String fieldName) {
        ctxField = fieldName;
        return this;
    }

    public String ctxField() {
        return ctxField;
    }

    public boolean hasCtxField() {
        return null != ctxField;
    }

    public ControllerClassMetaInfo addWith(String... classes) {
        int len = classes.length;
        if (len > 0) {
            for (int i = 0; i < len; ++i) {
                _addWith(classes[i]);
            }
        }
        return this;
    }

    public ControllerClassMetaInfo addBefore(InterceptorMethodMetaInfo before) {
        interceptors.addBefore(before);
        return this;
    }

    public ControllerClassMetaInfo addAfter(InterceptorMethodMetaInfo after) {
        interceptors.addAfter(after);
        return this;
    }

    public ControllerClassMetaInfo addCatch(CatchMethodMetaInfo cat) {
        interceptors.addCatch(cat);
        return this;
    }

    public ControllerClassMetaInfo addFinally(InterceptorMethodMetaInfo after) {
        interceptors.addFinally(after);
        return this;
    }

    public ControllerClassMetaInfo addInterceptor(InterceptorMethodMetaInfo info, Class<? extends Annotation> type) {
        interceptors.add(info, type);
        return this;
    }

    public ControllerClassMetaInfo addAction(ActionMethodMetaInfo info) {
        actions.add(info);
        return this;
    }

    public ActionMethodMetaInfo action(String name) {
        if (null == actionLookup) {
            for (ActionMethodMetaInfo act: actions) {
                if (S.eq(name, act.name())) {
                    return act;
                }
            }
            return null;
        }
        return actionLookup.get(name);
    }

    /**
     * Merge group interceptor list info from with classes into this class and
     * then merge interceptor list into action's interceptors
     */
    public ControllerClassMetaInfo merge(ControllerClassMetaInfoManager infoBase) {
        mergeFromWithList(infoBase);
        mergeIntoActionList();
        buildActionLookup();
        return this;
    }

    private void _addWith(String clsName) {
        withList.add(clsName);
    }

    private void mergeFromWithList(ControllerClassMetaInfoManager infoBase) {
        C.Set<String> withClasses = withList;
        for (String withClass : withClasses) {
            ControllerClassMetaInfo withClassInfo = infoBase.getControllerMetaInfo(withClass);
            if (null == withClassInfo) {
                withClass = Type.getType(withClass).getClassName();
                withClassInfo = infoBase.scanForControllerMetaInfo(withClass);
            }
            if (null != withClassInfo) {
                withClassInfo.merge(infoBase);
                interceptors.mergeFrom(withClassInfo.interceptors);
            } else {
                logger.warn("Cannot find class info for with class: %s", withClass);
            }
        }
    }

    private void mergeIntoActionList() {
        for (ActionMethodMetaInfo info: actions) {
            info.mergeFromClassInterceptors(interceptors);
        }
    }

    private void buildActionLookup() {
        C.Map<String, ActionMethodMetaInfo> lookup = C.newMap();
        for (ActionMethodMetaInfo act : actions) {
            lookup.put(act.name(), act);
        }
        actionLookup = lookup;
    }

    private static final C.Set<Class<? extends Annotation>> ACTION_ANNOTATION_TYPES = C.set(
            Action.class, GetAction.class, PostAction.class,
            PutAction.class, DeleteAction.class);

    private static final C.Set<Class<? extends Annotation>> INTERCEPTOR_ANNOTATION_TYPES = C.set(
            Before.class, After.class, Catch.class, Finally.class);

    public static final C.Set<H.Method> ACTION_METHODS = C.set(H.Method.GET, H.Method.POST, H.Method.PUT, H.Method.DELETE);

    public static boolean isActionAnnotation(Class<? extends Annotation> type) {
        return ACTION_ANNOTATION_TYPES.contains(type);
    }

    public static boolean isInterceptorAnnotation(Class<? extends Annotation> type) {
        return INTERCEPTOR_ANNOTATION_TYPES.contains(type);
    }

}
