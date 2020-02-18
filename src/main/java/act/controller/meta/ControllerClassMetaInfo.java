package act.controller.meta;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static act.Destroyable.Util.destroyAll;

import act.Act;
import act.app.App;
import act.app.AppClassLoader;
import act.asm.Type;
import act.cli.Command;
import act.controller.annotation.UrlContext;
import act.handler.builtin.controller.ControllerAction;
import act.handler.builtin.controller.Handler;
import act.plugin.ControllerPlugin;
import act.util.*;
import org.osgl.http.H;
import org.osgl.mvc.annotation.*;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.*;
import javax.enterprise.context.ApplicationScoped;

/**
 * Stores all class level information to support generating of
 * {@link ControllerAction request dispatcher}
 * and {@link Handler interceptors}
 */
@ApplicationScoped
public final class ControllerClassMetaInfo extends LogSupportedDestroyableBase {

    private Type type;
    private Type superType;
    private boolean isAbstract = false;
    private String ctxField = null;
    private boolean ctxFieldIsPrivate = true;
    private Set<String> withList = C.newSet();
    private List<ActionMethodMetaInfo> actions = new ArrayList<>();
    // actionLookup index action method by method name
    private Map<String, ActionMethodMetaInfo> actionLookup = null;
    // handlerLookup index handler method by method name
    // handler could by action or any kind of interceptors
    private Map<String, HandlerMethodMetaInfo> handlerLookup = null;
    GroupInterceptorMetaInfo interceptors = new GroupInterceptorMetaInfo();
    private ControllerClassMetaInfo parent;
    private boolean isController;
    private boolean possibleController;
    private String urlContext;
    private String templateContext;

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

    @Override
    public String toString() {
        return className();
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
        ControllerClassMetaInfo parentInfo = parent(true);
        if (null == parentInfo) {
            return false;
        }
        if (parentInfo.equals(clsInfo)) {
            return true;
        }
        return parentInfo.isMyAncestor(clsInfo);
    }

    public ControllerClassMetaInfo parent(ControllerClassMetaInfo parentInfo) {
        parent = parentInfo;
        return this;
    }

    public ControllerClassMetaInfo parent() {
        return parent;
    }

    public ControllerClassMetaInfo parent(boolean checkClassInfoRepo) {
        if (null != parent) {
            return parent;
        }
        if (!checkClassInfoRepo) {
            return null;
        }
        AppClassLoader classLoader = Act.app().classLoader();
        ClassInfoRepository repo = classLoader.classInfoRepository();
        ClassNode parentNode = repo.node(superType.getClassName());
        while(null != parentNode) {
            ControllerClassMetaInfo parentInfo = classLoader.controllerClassMetaInfo(parentNode.name());
            if (null != parentInfo) {
                return parentInfo;
            }
            parentNode = parentNode.parent();
        }
        return null;
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
        return handlerLookup.get(name);
    }

    <T extends InterceptorMethodMetaInfo> List<T> convertDerived(List<T> interceptors) {
        C.List<T> list = C.newSizedList(interceptors.size());
        for (InterceptorMethodMetaInfo derived : interceptors) {
            list.add((T)derived.extended(this));
        }
        return list;
    }

    public GroupInterceptorMetaInfo interceptors() {
        return interceptors;
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
        mergeFromSuperType(infoBase, app);
        mergeFromWithList(infoBase, app);
        mergeIntoActionList(infoBase, app);
        buildActionLookup();
        buildHandlerLookup();
        return this;
    }
    
    public String templateContext() {
        if (null != parent) {
            if (S.notBlank(templateContext) && templateContext.length() > 1 && templateContext.startsWith("/")) {
                return templateContext;
            }
            String parentContextPath = parent.templateContext();
            if (null == templateContext) {
                return parentContextPath;
            }
            if (null == parentContextPath) {
                return templateContext;
            }
            S.Buffer sb = S.newBuffer(parentContextPath);
            if (parentContextPath.endsWith("/")) {
                sb.deleteCharAt(sb.length() - 1);
            }
            if (!templateContext.startsWith("/")) {
                sb.append("/");
            }
            sb.append(templateContext);
            return sb.toString();
        }
        return templateContext;
    }

    public String urlContext() {
        if (null != parent) {
            if (S.notBlank(urlContext) && urlContext.length() > 1 && urlContext.startsWith("/")) {
                return urlContext;
            }
            String parentContextPath = parent.urlContext();
            if (null == urlContext) {
                return parentContextPath;
            }
            if (null == parentContextPath) {
                return urlContext;
            }
            S.Buffer sb = S.newBuffer(parentContextPath);
            if (parentContextPath.endsWith("/")) {
                sb.deleteCharAt(sb.length() - 1);
            }
            if (!urlContext.startsWith("/")) {
                sb.append("/");
            }
            sb.append(urlContext);
            return sb.toString();
        }
        return urlContext;
    }

    public ControllerClassMetaInfo templateContext(String path) {
        if (S.blank(path)) {
            templateContext = "/";
        } else {
            templateContext = path;
        }
        return this;
    }

    public ControllerClassMetaInfo urlContext(String path) {
        if (S.blank(path)) {
            urlContext = "/";
        } else {
            urlContext = path;
        }
        return this;
    }

    private void _addWith(String clsName) {
        withList.add(Type.getType(clsName).getClassName());
    }

    private void getAllWithList(final Set<String> withList, final ControllerClassMetaInfoManager infoBase) {
        withList.addAll(this.withList);
    }

    private ControllerClassMetaInfo superControllerClass(final ControllerClassMetaInfoManager infoBase) {
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
                if (null != info) {
                    if (info.isAbstract()) {
                        info = null;
                    } else {
                        return info;
                    }
                }
            }
            return info;
        }
        return null;
    }

    private void mergeFromSuperType(final ControllerClassMetaInfoManager infoBase, final App app) {
        ControllerClassMetaInfo superType = superControllerClass(infoBase);
        if (null == superType) {
            return;
        }
        superType.merge(infoBase, app);
        interceptors.mergeFrom(superType.interceptors, this);
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
                interceptors.mergeFrom(withClassInfo.interceptors);
            }
        }
    }

    private void mergeIntoActionList(ControllerClassMetaInfoManager infoBase, App app) {
        for (ActionMethodMetaInfo info : actions) {
            info.merge(infoBase, app);
            info.mergeFromClassInterceptors(interceptors);
        }
    }

    private void buildActionLookup() {
        Map<String, ActionMethodMetaInfo> lookup = new HashMap<>();
        for (ActionMethodMetaInfo act : actions) {
            lookup.put(act.name(), act);
        }
        actionLookup = lookup;
    }

    private void buildHandlerLookup() {
        Map<String, HandlerMethodMetaInfo> lookup = new HashMap<>();
        lookup.putAll(actionLookup);
        for (InterceptorMethodMetaInfo info : beforeInterceptors()) {
            lookup.put(info.name(), info);
        }
        for (InterceptorMethodMetaInfo info : afterInterceptors()) {
            lookup.put(info.name(), info);
        }
        for (InterceptorMethodMetaInfo info : exceptionInterceptors()) {
            lookup.put(info.name(), info);
        }
        for (InterceptorMethodMetaInfo info : finallyInterceptors()) {
            lookup.put(info.name(), info);
        }
        handlerLookup = lookup;
    }

    public static void registerMethodLookups(Map<Class<? extends Annotation>, H.Method> annotationMethodLookup, boolean noDefaultPath) {
        METHOD_LOOKUP.putAll(annotationMethodLookup);
        if (noDefaultPath) {
            NO_DEF_PATH_ACTIONS.addAll(annotationMethodLookup.keySet());
        }
    }

    public static void registerUrlContextAnnotation(ControllerPlugin.PathAnnotationSpec pathAnnotationInfo) {
        URL_CONTEXT_ANNOTATIONS.put(pathAnnotationInfo.annoType(), pathAnnotationInfo);
    }

    private static final C.Set<Class<? extends Annotation>> INTERCEPTOR_ANNOTATION_TYPES = C.set(
            Before.class, After.class, Catch.class, Finally.class);

    public static final C.Set<H.Method> ACTION_METHODS = C.set(H.Method.GET, H.Method.POST, H.Method.PUT, H.Method.DELETE);

    private static final Map<Class<? extends Annotation>, H.Method> METHOD_LOOKUP = C.newMap(
            GetAction.class, H.Method.GET,
            PostAction.class, H.Method.POST,
            PutAction.class, H.Method.PUT,
            DeleteAction.class, H.Method.DELETE,
            PatchAction.class, H.Method.PATCH,
            WsAction.class, H.Method.GET,
            Command.class, H.Method.GET
    );

    private static final Set<Class<? extends Annotation>> NO_DEF_PATH_ACTIONS = new HashSet<>();

    public static boolean noDefPath(Class<? extends Annotation> actionAnno) {
        return NO_DEF_PATH_ACTIONS.contains(actionAnno);
    }

    // map annotype to support_absolute_path
    private static final Map<Class<? extends Annotation>, ControllerPlugin.PathAnnotationSpec> URL_CONTEXT_ANNOTATIONS = new HashMap<>();

    public static boolean isActionAnnotation(Class<? extends Annotation> type) {
        return METHOD_LOOKUP.containsKey(type) || Action.class == type;
    }

    public static boolean isUrlContextAnnotation(Class<? extends Annotation> anno) {
        return UrlContext.class == anno || URL_CONTEXT_ANNOTATIONS.containsKey(anno);
    }

    public static boolean isUrlContextAnnotationSupportAbsolutePath(Class<? extends Annotation> anno) {
        return UrlContext.class == anno || URL_CONTEXT_ANNOTATIONS.get(anno).supportAbsolutePath();
    }

    public static boolean isUrlContextAnnotationSupportInheritance(Class<? extends Annotation> anno) {
        return UrlContext.class == anno || URL_CONTEXT_ANNOTATIONS.get(anno).supportInheritance();
    }

    public static H.Method lookupHttpMethod(Class annotationClass) {
        return METHOD_LOOKUP.get(annotationClass);
    }

    public static boolean isActionUtilAnnotation(Class<? extends Annotation> type) {
        return ActionUtil.class == type;
    }

    public static boolean isInterceptorAnnotation(Class<? extends Annotation> type) {
        return INTERCEPTOR_ANNOTATION_TYPES.contains(type);
    }

}
