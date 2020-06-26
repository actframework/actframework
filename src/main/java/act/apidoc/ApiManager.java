package act.apidoc;

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

import static act.controller.Controller.Util.renderJson;

import act.Act;
import act.apidoc.Endpoint.ParamInfo;
import act.apidoc.javadoc.*;
import act.app.*;
import act.app.event.SysEventId;
import act.app.util.NamedPort;
import act.conf.AppConfig;
import act.handler.RequestHandler;
import act.handler.RequestHandlerBase;
import act.handler.builtin.ResourceGetter;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.inject.util.ResourceLoader;
import act.route.RouteSource;
import act.route.Router;
import act.util.FastJsonFileSerializer;
import act.util.FastJsonSObjectSerializer;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NamedNode;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.exception.NotAppliedException;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.*;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keep track endpoints defined in the system
 */
public class ApiManager extends AppServiceBase<ApiManager> {

    private static final String FILENAME = ".act.api-book";

    static final Logger LOGGER = LogManager.get(ApiManager.class);

    /**
     * The {@link Endpoint} defined in the system
     */
    SortedSet<Endpoint> endpoints = new TreeSet<>();

    /**
     * Mapped by {@link Endpoint#getId()}
     */
    Map<String, Endpoint> endpointLookup = new HashMap<>();

    SortedMap<String, List<Endpoint>> moduleLookup = new TreeMap<>();

    private transient boolean enabled;

    private static final AtomicBoolean IN_PROGRESS = new AtomicBoolean(false);

    public static boolean inProgress() {
        return IN_PROGRESS.get();
    }

    public ApiManager(final App app) {
        super(app);
        this.enabled = app.config().apiDocEnabled() && !Act.isTest();
        if (!this.enabled) {
            return;
        }
        app.jobManager().post(SysEventId.POST_START, "compile-api-book", new Runnable() {
            @Override
            public void run() {
                IN_PROGRESS.set(true);
                try {
                    load(app);
                } finally {
                    IN_PROGRESS.set(false);
                }
            }
        });
        Router router = app.isDev() ? app.router() : app.sysRouter();
        router.addMapping(H.Method.GET, "/~/apibook/endpoints", new GetEndpointsHandler(this));
        router.addMapping(H.Method.GET, "/~/apibook/modules", new GetModulesHandler(this));
        ResourceGetter apidocHandler = new ResourceGetter("asset/~act/apibook/index.html");
        router.addMapping(H.Method.GET, "/~/api", apidocHandler);
        router.addMapping(H.Method.GET, "/~/apibook", apidocHandler);
        router.addMapping(H.Method.GET, "/~/apidoc", apidocHandler);
    }

    @Override
    protected void releaseResources() {
        endpoints.clear();
        moduleLookup.clear();
    }

    public Endpoint endpoint(String id) {
        return endpointLookup.get(id);
    }

    public void load(App app) {
        LOGGER.info("start compiling API book");
        if (app.isProd()) {
            try {
                deserialize();
            } catch (Exception e) {
                warn(e, "Error deserialize api-book");
            }
            if (!endpoints.isEmpty()) {
                return;
            }
        }
        loadActAppDocs();
        Router router = app.router();
        AppConfig config = app.config();
        Set<Class> controllerClasses = new HashSet<>();
        ApiDocCompileContext ctx = new ApiDocCompileContext();
        ctx.saveCurrent();
        SerializeConfig fjConfig = SerializeConfig.globalInstance;
        Class<?> stringSObjectType = SObject.of("").getClass();
        fjConfig.put(stringSObjectType, new FastJsonSObjectSerializer());
        try {
            load(router, null, config, controllerClasses, ctx);
            for (NamedPort port : app.config().namedPorts()) {
                router = app.router(port);
                load(router, port, config, controllerClasses, ctx);
            }
            if (Act.isDev()) {
                exploreDescriptions(controllerClasses);
            }
            buildModuleLookup();
            serialize();
        } finally {
            ctx.destroy();
        }
        LOGGER.info("API book compiled");
    }

    private void loadActAppDocs() {
        URL url = ApiManager.class.getResource("/act/act.api-book");
        if (null != url) {
            deserialize(IO.read(url).toString(), true);
        }
    }

    private void serialize() {
        File file = new File(FILENAME);
        IO.write(JSON.toJSONString(moduleLookup)).to(file);
    }

    private void deserialize() {
        File file = new File(FILENAME);
        if (!file.exists() || !file.canRead()) {
            return;
        }
        deserialize(IO.readContentAsString(file), false);
    }

    private void deserialize(String content, boolean actDoc) {
        SortedMap<String, List<Endpoint>> moduleLookup = actDoc ? new TreeMap<String, List<Endpoint>>() : this.moduleLookup;
        JSONObject jsonObject = JSON.parseObject(content);
        $.map(jsonObject).instanceFactory(new Lang.Function<Class, Object>() {
            @Override
            public Object apply(Class aClass) throws NotAppliedException, Lang.Break {
                return app().getInstance(aClass);
            }
        }).targetGenericType(new TypeReference<TreeMap<String, List<Endpoint>>>() {
        }).to(moduleLookup);
        if (!actDoc) {
            for (List<Endpoint> list : moduleLookup.values()) {
                endpoints.addAll(list);
            }
            for (Endpoint endpoint : endpoints) {
                endpointLookup.put(endpoint.getId(), endpoint);
            }
        } else {
            for (List<Endpoint> list : moduleLookup.values()) {
                for (Endpoint endpoint : list) {
                    endpointLookup.put(endpoint.getId(), endpoint);
                }
            }
        }
    }

    private void buildModuleLookup() {
        for (Endpoint endpoint : endpoints) {
            String module = endpoint.getModule();
            List<Endpoint> list = moduleLookup.get(module);
            if (null == list) {
                list = new ArrayList<>();
                moduleLookup.put(module, list);
            }
            list.add(endpoint);
        }
    }

    private void load(Router router, NamedPort port, AppConfig config, final Set<Class> controllerClasses, final ApiDocCompileContext ctx) {
        final int portNumber = null == port ? config.httpExternalPort() : port.port();
        final boolean isDev = Act.isDev();
        final boolean hideBuiltIn = app().config().isHideBuiltInEndpointsInApiDoc();
        router.accept(new Router.Visitor() {
            @Override
            public void visit(H.Method method, String path, RouteSource routeSource, Set<String> varNames, RequestHandler handler) {
                ctx.routeSource(routeSource);
                if (showEndpoint(path, handler)) {
                    Endpoint endpoint = new Endpoint(portNumber, method, path,varNames, handler);
                    endpoints.add(endpoint);
                    endpointLookup.put(endpoint.getId(), endpoint);
                    if (isDev) {
                        controllerClasses.add(endpoint.controllerClass());
                    }
                }
            }

            private boolean showEndpoint(String path, RequestHandler handler) {
                return (handler instanceof RequestHandlerProxy)
                        && !(hideBuiltIn && path.startsWith("/~/"));
            }
        });
    }

    private Set<Class> withSuperClasses(Set<Class> classes) {
        Set<Class> ret = new HashSet<>(classes);
        for (Class c : classes) {
            c = c.getSuperclass();
            while (c != null && c != Object.class) {
                ret.add(c);
                c = c.getSuperclass();
            }
        }
        return ret;
    }

    private static List<JavadocBlockTag> merge(List<JavadocBlockTag> overwritten, List<JavadocBlockTag> parent) {
        if (null == overwritten || overwritten.isEmpty()) {
            return parent;
        }
        List<JavadocBlockTag> list = new ArrayList<>(overwritten);
        Map<$.T2<JavadocBlockTag.Type, $.Option<String>>, JavadocBlockTag> parentTagLookup = new HashMap<>();
        for (JavadocBlockTag tag : parent) {
            parentTagLookup.put($.T2(tag.getType(), tag.getName()), tag);
        }
        for (JavadocBlockTag tag : overwritten) {
            JavadocBlockTag parentTag = parentTagLookup.remove($.T2(tag.getType(), tag.getName()));
            if (tag.getContent().isEmpty()) {
                if (null != parentTag) {
                    tag.setContent(parentTag.getContent());
                }
            }
            list.add(tag);
        }
        list.addAll(parentTagLookup.values());
        return list;
    }

    private Javadoc javadocOf(Endpoint endpoint, Map<String, Javadoc> methodJavaDocs) {
        Javadoc javadoc = methodJavaDocs.get(endpoint.getId().replace('$', '.'));
        if (null == javadoc) {
            String parentId = endpoint.getParentId();
            if (S.blank(parentId)) {
                return null;
            }
            Endpoint parent = endpointLookup.get(parentId);
            return null == parent ? methodJavaDocs.get(parentId) : javadocOf(parent, methodJavaDocs);
        }
        JavadocDescription desc = javadoc.getDescription();
        String s = desc.toText();
        if (S.blank(s) || s.contains("@inheritDoc")) {
            String parentId = endpoint.getParentId();
            if (S.notBlank(parentId)) {
                Endpoint parent = endpointLookup.get(parentId);
                Javadoc parentDoc = null == parent ? methodJavaDocs.get(parentId) : javadocOf(parent, methodJavaDocs);
                if (null != parentDoc) {
                    if (S.blank(s)) {
                        javadoc = parentDoc;
                    } else {
                        JavadocDescription parentDesc = parentDoc.getDescription();
                        s = s.replace("@inheritDoc", parentDesc.toText());
                        javadoc = new Javadoc(JavadocDescription.parseText(s), merge(javadoc.blockTags(), parentDoc.blockTags()));
                    }
                }
            }
        }
        return javadoc;
    }

    private void exploreDescriptions(Set<Class> controllerClasses) {
        DevModeClassLoader cl = $.cast(Act.app().classLoader());
        Map<String, Javadoc> methodJavaDocs = new HashMap<>();
        Map<String, Javadoc> fieldJavaDocs = new HashMap<>();
        for (Class controllerClass: withSuperClasses(controllerClasses)) {
            Source src = cl.source(controllerClass);
            if (null == src) {
                continue;
            }
            try {
                CompilationUnit compilationUnit = JavaParser.parse(IO.reader(src.code()), true);
                List<TypeDeclaration> types = compilationUnit.getTypes();
                for (TypeDeclaration type : types) {
                    if (type instanceof ClassOrInterfaceDeclaration) {
                        exploreDeclaration((ClassOrInterfaceDeclaration) type, methodJavaDocs, fieldJavaDocs);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn(e, "error parsing source for " + controllerClass);
            }
        }
        for (Endpoint endpoint : endpoints) {
            Javadoc javadoc = javadocOf(endpoint, methodJavaDocs);
            List<ParamInfo> params = endpoint.getParams();
            Map<String, ParamInfo> paramLookup = new HashMap<>();
            for (ParamInfo param : params) {
                String paramName = param.bindName;
                paramLookup.put(paramName, param);
                String fieldKey = param.fieldKey;
                Javadoc fieldJavadoc = fieldJavaDocs.get(fieldKey);
                if (null != fieldJavadoc) {
                    JavadocDescription fieldJavadocDesc = fieldJavadoc.getDescription();
                    if (null != fieldJavadocDesc) {
                        param.setDescription(endpoint.processTypeImplSubstitution(fieldJavadocDesc.toText()));
                    }
                }
            }
            if (null != javadoc) {
                String desc = javadoc.getDescription().toText();
                if (S.notBlank(desc)) {
                    endpoint.description = endpoint.processTypeImplSubstitution(desc);
                }
                List<JavadocBlockTag> blockTags = javadoc.getBlockTags();
                String returnDesc = null;
                for (JavadocBlockTag tag : blockTags) {
                    if ("param".equals(tag.getTagName())) {
                        String paramName = tag.getName().get();
                        ParamInfo paramInfo = paramLookup.get(paramName);
                        if (null == paramInfo) {
                            paramInfo = paramLookup.get(paramName + " (body)");
                        }
                        if (null != paramInfo) {
                            String paramDesc = endpoint.processTypeImplSubstitution(tag.getContent().toText());
                            if (S.blank(paramDesc)) {
                                paramDesc = "JSON body of " + paramName;
                            }
                            paramInfo.setDescription(paramDesc);
                        }
                    } else if ("return".equals(tag.getTagName())) {
                        returnDesc = tag.getContent().toText();
                    }
                }
                if (null != returnDesc) {
                    endpoint.returnDescription = endpoint.processTypeImplSubstitution(returnDesc);
                }
            } else {
                // try check if we have Act built-in super class
                Endpoint parentEndpoint = endpointLookup.get(endpoint.getParentId());
                if (null != parentEndpoint) {
                    endpoint.description = endpoint.processTypeImplSubstitution(parentEndpoint.description);
                    endpoint.returnDescription = endpoint.processTypeImplSubstitution(parentEndpoint.returnDescription);
                    Map<String, ParamInfo> parentEndpointParamLookup = new HashMap<>();
                    for (ParamInfo param : parentEndpoint.params) {
                        parentEndpointParamLookup.put(param.getName(), param);
                    }
                    for (ParamInfo param : endpoint.params) {
                        ParamInfo parentParam = parentEndpointParamLookup.get(param.getName());
                        if (null != parentParam) {
                            param.description = endpoint.processTypeImplSubstitution(parentParam.description);
                        }
                    }
                }
            }
        }
    }

    private static String name(Node node) {
        S.Buffer buffer = S.newBuffer();
        Node parent = node.getParentNode();
        if (null != parent) {
            buffer.append(name(parent));
        }
        if (node instanceof NamedNode) {
            buffer.append(".").append(((NamedNode) node).getName());
        } else if (node instanceof CompilationUnit) {
            CompilationUnit unit = $.cast(node);
            PackageDeclaration pkg = unit.getPackage();
            return pkg.getPackageName();
        }
        return buffer.toString();
    }

    private void exploreDeclaration(
            ClassOrInterfaceDeclaration classDeclaration,
            Map<String, Javadoc> methodJavaDocs,
            Map<String, Javadoc> fieldJavaDocs
    ) {
        String prefix = name(classDeclaration);
        for (Node node : classDeclaration.getChildrenNodes()) {
            if (node instanceof ClassOrInterfaceDeclaration) {
                exploreDeclaration((ClassOrInterfaceDeclaration) node, methodJavaDocs, fieldJavaDocs);
            } else if (node instanceof FieldDeclaration) {
                FieldDeclaration fieldDeclaration = (FieldDeclaration) node;
                Comment comment = fieldDeclaration.getComment();
                if (!(comment instanceof JavadocComment)) {
                    continue;
                }
                List<VariableDeclarator> vars = fieldDeclaration.getVariables();
                if (vars.size() > 0) {
                    JavadocComment javadocComment = (JavadocComment) comment;
                    Javadoc javadoc = JavadocParser.parse(javadocComment);
                    fieldJavaDocs.put(S.concat(prefix, ".", vars.get(0).getId()), javadoc);
                }
            } else if (node instanceof MethodDeclaration) {
                MethodDeclaration methodDeclaration = (MethodDeclaration) node;
                // Note we can't check only request handler annotation here
                // because we need to cater for the extended request handler declaration
                // which does not have the annotation specified
                boolean needJavadoc = Modifier.isPublic(methodDeclaration.getModifiers());
                if (!needJavadoc) {
                    continue;
                }
                Comment comment = methodDeclaration.getComment();
                if (!(comment instanceof JavadocComment)) {
                    continue;
                }
                JavadocComment javadocComment = (JavadocComment) comment;
                Javadoc javadoc = JavadocParser.parse(javadocComment);
                methodJavaDocs.put(S.concat(prefix, ".", methodDeclaration.getName()), javadoc);
            }
        }
    }

    private class GetEndpointsHandler extends RequestHandlerBase {

        private ApiManager api;

        public GetEndpointsHandler(ApiManager api) {
            this.api = api;
        }

        @Override
        public void handle(ActionContext context) {
            String module = context.paramVal("module");
            Collection<Endpoint> endpoints = S.notBlank(module) ? api.moduleLookup.get(module) : api.endpoints;
            renderJson(endpoints).apply(context.req(), context.prepareRespForResultEvaluation());
        }

        @Override
        public void prepareAuthentication(ActionContext context) {

        }

        @Override
        public String toString() {
            return "API doc handler";
        }
    }

    private class GetModulesHandler extends RequestHandlerBase {

        private ApiManager api;

        public GetModulesHandler(ApiManager api) {
            this.api = api;
        }

        @Override
        public void handle(ActionContext context) {
            renderJson(api.moduleLookup.keySet()).apply(context.req(), context.prepareRespForResultEvaluation());
        }

        @Override
        public void prepareAuthentication(ActionContext context) {

        }
        @Override
        public String toString() {
            return "API doc module index";
        }
    }


}
