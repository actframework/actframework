package act.controller.meta;

import act.app.App;
import act.asm.Type;
import act.handler.builtin.controller.ControllerAction;
import act.handler.builtin.controller.Handler;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import act.util.DestroyableBase;
import org.osgl.http.H;
import org.osgl.mvc.annotation.*;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import static act.Destroyable.Util.destroyAll;

/**
 * Stores all class level information to support generating of
 * {@link ControllerAction request dispatcher}
 * and {@link Handler interceptors}
 */
@ApplicationScoped
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
    private boolean isController;
    private boolean possibleController;
    private String contextPath;

    public ControllerClassMetaInfo className(String name) {
        this.type = Type.getObjectType(name);
        return this;
    }

    @Override
    protected void releaseResources() {
        withList.clear();
        destroyAll(actions, ApplicationScoped.class);
        actions.clear();
        if (null != actionLookup) {
            destroyAll(actionLookup.values(), ApplicationScoped.class);
            actionLookup.clear();
        }
        if (null != handlerLookup) {
            destroyAll(handlerLookup.values(), ApplicationScoped.class);
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

    public boolean possibleController() {
        return possibleController;
    }

    public ControllerClassMetaInfo possibleController(boolean b) {
        possibleController = b;
        return this;
    }

    boolean isMyAncestor(ControllerClassMetaInfo clsInfo) {
        if (parent == null) {
            return false;
        }
        if (parent.equals(clsInfo)) {
            return true;
        }
        return parent.isMyAncestor(clsInfo);
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

    <T extends InterceptorMethodMetaInfo> List<T> convertDerived(List<T> interceptors) {
        C.List<T> list = C.newSizedList(interceptors.size());
        for (InterceptorMethodMetaInfo derived : interceptors) {
            list.add((T)derived.extended(this));
        }
        return list;
    }

    public List<InterceptorMethodMetaInfo> beforeInterceptors() {
        return interceptors.beforeList();
    }

    public List<InterceptorMethodMetaInfo> afterInterceptors() {
        return interceptors.afterList();
    }

    public List<CatchMethodMetaInfo> exceptionInterceptors() {
        return interceptors.catchList();
    }

    public List<InterceptorMethodMetaInfo> finallyInterceptors() {
        return interceptors.finallyList();
    }

    public ControllerClassMetaInfo merge(ControllerClassMetaInfoManager infoBase, App app) {
        mergeFromWithList(infoBase, app);
        mergeIntoActionList();
        buildActionLookup();
        buildHandlerLookup(app);
        return this;
    }

    public String contextPath() {
        if (null != parent) {
            if (S.notBlank(contextPath) && contextPath.length() > 1 && contextPath.startsWith("/")) {
                return contextPath;
            }
            String parentContextPath = parent.contextPath();
            if (null == contextPath) {
                return parentContextPath;
            }
            StringBuilder sb = S.builder(parentContextPath);
            if (parentContextPath.endsWith("/")) {
                sb.deleteCharAt(sb.length() - 1);
            }
            if (!contextPath.startsWith("/")) {
                sb.append("/");
            }
            sb.append(contextPath);
            return sb.toString();
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

    private void getAllWithList(final Set<String> withList, final ControllerClassMetaInfoManager infoBase) {
        withList.addAll(this.withList);
        if (null != superType) {
            final String superClass = superType.getClassName();
            App app = App.instance();
            ClassInfoRepository repo = app.classLoader().classInfoRepository();
            ControllerClassMetaInfo info = infoBase.controllerMetaInfo(superClass);
            String curSuperClass = superClass;
            while (null == info && !"java.lang.Object".equals(curSuperClass)) {
                ClassNode node = repo.node(curSuperClass);
                if (null != node) {
                    node = node.parent();
                }
                if (null == node) {
                    break;
                }
                curSuperClass = node.name();
                info = infoBase.controllerMetaInfo(curSuperClass);
            }
            if (null != info) {
                withList.add(superClass);
            }
        }
    }

    private void mergeFromWithList(final ControllerClassMetaInfoManager infoBase, final App app) {
        C.Set<String> withClasses = C.newSet();
        getAllWithList(withClasses, infoBase);
        final ControllerClassMetaInfo me = this;
        ClassInfoRepository repo = app.classLoader().classInfoRepository();
        for (final String withClass : withClasses) {
            String curWithClass = withClass;
            ControllerClassMetaInfo withClassInfo = infoBase.controllerMetaInfo(curWithClass);
            while (null == withClassInfo && !"java.lang.Object".equals(curWithClass)) {
                ClassNode node = repo.node(curWithClass);
                if (null != node) {
                    node = node.parent();
                }
                if (null == node) {
                    break;
                }
                curWithClass = node.name();
                withClassInfo = infoBase.controllerMetaInfo(curWithClass);
            }
            if (null != withClassInfo) {
                withClassInfo.merge(infoBase, app);
                if (isMyAncestor(withClassInfo)) {
                    interceptors.mergeFrom(withClassInfo.interceptors, me);
                } else {
                    interceptors.mergeFrom(withClassInfo.interceptors);
                }
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
        for (InterceptorMethodMetaInfo info : beforeInterceptors()) {
            lookup.put(info.fullName(), info);
        }
        for (InterceptorMethodMetaInfo info : afterInterceptors()) {
            lookup.put(info.fullName(), info);
        }
        for (InterceptorMethodMetaInfo info : exceptionInterceptors()) {
            lookup.put(info.fullName(), info);
        }
        for (InterceptorMethodMetaInfo info : finallyInterceptors()) {
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
