package act.controller.meta;

import act.app.App;
import act.app.AppClassLoader;
import act.asm.Type;
import act.handler.builtin.controller.ControllerAction;
import act.handler.builtin.controller.Handler;
import act.util.DestroyableBase;
import org.osgl.http.H;
import org.osgl.mvc.annotation.*;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import static act.Destroyable.Util.destroyAll;

/**
 * Stores all class level information to support generating of
 * {@link ControllerAction request dispatcher}
 * and {@link Handler interceptors}
 */
public final class ControllerClassMetaInfo extends DestroyableBase {

    private Type type;
    private Type superType;
    private boolean isAbstract = false;
    private String ctxField = null;
    private boolean ctxFieldIsPrivate = true;
    private C.Set<String> withList = C.newSet();
    private C.List<ActionMethodMetaInfo> actions = C.newList();
    // actionLookup index action method by method name
    private C.Map<String, ActionMethodMetaInfo> actionLookup = null;
    // handlerLookup index handler method by method name
    // handler could by action or any kind of interceptors
    private C.Map<String, HandlerMethodMetaInfo> handlerLookup = null;
    private GroupInterceptorMetaInfo interceptors = new GroupInterceptorMetaInfo();
    private ControllerClassMetaInfo parent;
    private C.Map<String, FieldPathVariableInfo> fieldPathVariableInfoMap = C.newMap();
    private boolean isController;
    private String contextPath;

    public ControllerClassMetaInfo className(String name) {
        this.type = Type.getObjectType(name);
        return this;
    }

    @Override
    protected void releaseResources() {
        withList.clear();
        destroyAll(actions);
        actions.clear();
        if (null != actionLookup) {
            destroyAll(actionLookup.values());
            actionLookup.clear();
        }
        if (null != handlerLookup) {
            destroyAll(handlerLookup.values());
            handlerLookup.clear();
        }
        interceptors.destroy();
        if (null != parent) parent.destroy();
        super.releaseResources();
    }

    public String className() {
        return type.getClassName();
    }

    public String internalName() {
        return type.getInternalName();
    }

    public Type type() {
        return type;
    }

    public ControllerClassMetaInfo superType(Type type) {
        superType = type;
        return this;
    }

    public Type superType() {
        return superType;
    }

    public List<String> withList() {
        return C.list(withList);
    }

    public ControllerClassMetaInfo addFieldPathVariableInfo(FieldPathVariableInfo info) {
        fieldPathVariableInfoMap.put(info.fieldName(), info);
        return this;
    }

    public FieldPathVariableInfo fieldPathVariableInfo(String name) {
        return fieldPathVariableInfoMap.get(name);
    }

    public List<FieldPathVariableInfo> fieldPathVariableInfos() {
        C.List<FieldPathVariableInfo> list = C.list(fieldPathVariableInfoMap.values());
        ControllerClassMetaInfo p = parent;
        if (null != p) {
            list = list.append(p.fieldPathVariableInfos());
        }
        return list;
    }


    public ControllerClassMetaInfo setAbstract() {
        isAbstract = true;
        return this;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isController() {
        return isController;
    }

    public ControllerClassMetaInfo isController(boolean b) {
        isController = b;
        return this;
    }

    public ControllerClassMetaInfo parent(ControllerClassMetaInfo parentInfo) {
        parent = parentInfo;
        return this;
    }

    public ControllerClassMetaInfo ctxField(String fieldName, boolean isPrivate) {
        ctxField = fieldName;
        ctxFieldIsPrivate = isPrivate;
        return this;
    }

    public String nonPrivateCtxField() {
        if (null != ctxField) {
            return ctxFieldIsPrivate ? null : ctxField;
        }
        return null == parent ? null : parent.nonPrivateCtxField();
    }

    public String ctxField() {
        if (null != ctxField) {
            return ctxField;
        }
        if (null != parent) {
            return parent.nonPrivateCtxField();
        }
        return null;
    }

    public boolean hasCtxField() {
        return null != ctxField;
    }

    public boolean ctxFieldIsPrivate() {
        return ctxFieldIsPrivate;
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
            for (ActionMethodMetaInfo act : actions) {
                if (S.eq(name, act.name())) {
                    return act;
                }
            }
            return null;
        }
        return actionLookup.get(name);
    }

    public HandlerMethodMetaInfo handler(String name) {
        HandlerMethodMetaInfo info;
        if (null == handlerLookup) {
            info = action(name);
            if (null != info) {
                return info;
            }
            return interceptors.find(name, className());
        }
        return handlerLookup.get(className() + "." + name);
    }

    public List<InterceptorMethodMetaInfo> beforeInterceptors(App app) {
        C.List<InterceptorMethodMetaInfo> list = C.newList();
        AppClassLoader cl = app.classLoader();
        for (String with : withList) {
            ControllerClassMetaInfo withInfo = cl.controllerClassMetaInfo(with);
            if (null != withInfo) {
                list.addAll(withInfo.beforeInterceptors(app));
            }
        }
        list.addAll(interceptors.beforeList());
        return list;
    }

    public List<InterceptorMethodMetaInfo> afterInterceptors(App app) {
        C.List<InterceptorMethodMetaInfo> list = C.newList();
        AppClassLoader cl = app.classLoader();
        for (String with : withList) {
            ControllerClassMetaInfo withInfo = cl.controllerClassMetaInfo(with);
            if (null != withInfo) {
                list.addAll(withInfo.afterInterceptors(app));
            }
        }
        list.addAll(interceptors.afterList());
        return list;
    }

    public List<CatchMethodMetaInfo> exceptionInterceptors(App app) {
        C.List<CatchMethodMetaInfo> list = C.newList();
        AppClassLoader cl = app.classLoader();
        for (String with : withList) {
            ControllerClassMetaInfo withInfo = cl.controllerClassMetaInfo(with);
            if (null != withInfo) {
                list.addAll(withInfo.exceptionInterceptors(app));
            }
        }
        list.addAll(interceptors.catchList());
        return list;
    }

    public List<InterceptorMethodMetaInfo> finallyInterceptors(App app) {
        C.List<InterceptorMethodMetaInfo> list = C.newList();
        AppClassLoader cl = app.classLoader();
        for (String with : withList) {
            ControllerClassMetaInfo withInfo = cl.controllerClassMetaInfo(with);
            if (null != withInfo) {
                list.addAll(withInfo.finallyInterceptors(app));
            }
        }
        list.addAll(interceptors.finallyList());
        return list;
    }

    public ControllerClassMetaInfo merge(ControllerClassMetaInfoManager infoBase, App app) {
        mergeFromWithList(infoBase, app);
        mergeIntoActionList();
        buildActionLookup();
        buildHandlerLookup(app);
        return this;
    }

    public String contextPath() {
        if (null != parent && (S.blank(contextPath) || "/".equals(contextPath))) {
            return parent.contextPath();
        }
        return contextPath;
    }

    public ControllerClassMetaInfo contextPath(String path) {
        if (S.blank(path)) {
            contextPath = "/";
        } else {
            contextPath = path;
        }
        return this;
    }

    private void _addWith(String clsName) {
        withList.add(Type.getType(clsName).getClassName());
    }

    private void getAllWithList(Set<String> withList, ControllerClassMetaInfoManager infoBase) {
        withList.addAll(this.withList);
        if (null != superType) {
            String superClass = superType.getClassName();
            ControllerClassMetaInfo info = infoBase.controllerMetaInfo(superClass);
            if (null != info) {
                info.getAllWithList(withList, infoBase);
                withList.add(superClass);
            }
        }
    }

    private void mergeFromWithList(ControllerClassMetaInfoManager infoBase, App app) {
        C.Set<String> withClasses = C.newSet();
        getAllWithList(withClasses, infoBase);
        for (String withClass : withClasses) {
            ControllerClassMetaInfo withClassInfo = infoBase.controllerMetaInfo(withClass);
            if (null != withClassInfo) {
                withClassInfo.merge(infoBase, app);
                interceptors.mergeFrom(withClassInfo.interceptors);
            } else {
                logger.warn("Cannot find class info for @With class: %s", withClass);
            }
        }
    }

    private void mergeIntoActionList() {
        for (ActionMethodMetaInfo info : actions) {
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

    private void buildHandlerLookup(App app) {
        C.Map<String, HandlerMethodMetaInfo> lookup = C.newMap();
        lookup.putAll(actionLookup);
        for (InterceptorMethodMetaInfo info : beforeInterceptors(app)) {
            lookup.put(info.fullName(), info);
        }
        for (InterceptorMethodMetaInfo info : afterInterceptors(app)) {
            lookup.put(info.fullName(), info);
        }
        for (InterceptorMethodMetaInfo info : exceptionInterceptors(app)) {
            lookup.put(info.fullName(), info);
        }
        for (InterceptorMethodMetaInfo info : finallyInterceptors(app)) {
            lookup.put(info.fullName(), info);
        }
        handlerLookup = lookup;
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
