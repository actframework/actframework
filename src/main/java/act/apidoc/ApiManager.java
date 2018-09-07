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
import act.route.Router;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keep track endpoints defined in the system
 */
public class ApiManager extends AppServiceBase<ApiManager> {

    static final Logger LOGGER = LogManager.get(ApiManager.class);

    /**
     * The {@link Endpoint} defined in the system
     */
    SortedSet<Endpoint> endpoints = new TreeSet<>();

    SortedMap<String, List<Endpoint>> moduleLookup = new TreeMap<>();

    private static final AtomicBoolean IN_PROGRESS = new AtomicBoolean(false);

    public static boolean inProgress() {
        return IN_PROGRESS.get();
    }

    public ApiManager(final App app) {
        super(app);
        if (!app.config().apiDocEnabled()) {
            return;
        }
        app.jobManager().alongWith(SysEventId.POST_START, "compile-api-book", new Runnable() {
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
        app.router().addMapping(H.Method.GET, "/~/apibook/endpoints", new GetEndpointsHandler(this));
        app.router().addMapping(H.Method.GET, "/~/apibook/modules", new GetModulesHandler(this));
        ResourceGetter apidocHandler = new ResourceGetter("asset/~act/apibook/index.html");
        app.router().addMapping(H.Method.GET, "/~/apibook", apidocHandler);
        app.router().addMapping(H.Method.GET, "/~/apidoc", apidocHandler);
    }

    @Override
    protected void releaseResources() {
        endpoints.clear();
        moduleLookup.clear();
    }

    public void load(App app) {
        LOGGER.info("start compiling API book");
        Router router = app.router();
        AppConfig config = app.config();
        Set<Class> controllerClasses = new HashSet<>();
        load(router, null, config, controllerClasses);
        for (NamedPort port : app.config().namedPorts()) {
            router = app.router(port);
            load(router, port, config, controllerClasses);
        }
        if (Act.isDev()) {
            exploreDescriptions(controllerClasses);
        }
        buildModuleLookup();
        LOGGER.info("API book compiled");
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

    private void load(Router router, NamedPort port, AppConfig config, final Set<Class> controllerClasses) {
        final int portNumber = null == port ? config.httpExternalPort() : port.port();
        final boolean isDev = Act.isDev();
        final boolean hideBuiltIn = app().config().isHideBuiltInEndpointsInApiDoc();
        router.accept(new Router.Visitor() {
            @Override
            public void visit(H.Method method, String path, RequestHandler handler) {
                if (showEndpoint(path, handler)) {
                    Endpoint endpoint = new Endpoint(portNumber, method, path, handler);
                    endpoints.add(endpoint);
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

    private void exploreDescriptions(Set<Class> controllerClasses) {
        DevModeClassLoader cl = $.cast(Act.app().classLoader());
        Map<String, Javadoc> methodJavaDocs = new HashMap<>();
        for (Class controllerClass: controllerClasses) {
            Source src = cl.source(controllerClass);
            if (null == src) {
                continue;
            }
            try {
                CompilationUnit compilationUnit = JavaParser.parse(IO.reader(src.code()), true);
                List<TypeDeclaration> types = compilationUnit.getTypes();
                for (TypeDeclaration type : types) {
                    if (type instanceof ClassOrInterfaceDeclaration) {
                        exploreDeclaration((ClassOrInterfaceDeclaration) type, methodJavaDocs, "");
                    }
                }
            } catch (Exception e) {
                LOGGER.warn(e, "error parsing source for " + controllerClass);
            }
        }
        for (Endpoint endpoint : endpoints) {
            Javadoc javadoc = methodJavaDocs.get(endpoint.getId());
            if (null != javadoc) {
                String desc = javadoc.getDescription().toText();
                if (S.notBlank(desc)) {
                    endpoint.setDescription(desc);
                }
                List<ParamInfo> params = endpoint.getParams();
                if (params.isEmpty()) {
                    continue;
                }
                Map<String, ParamInfo> paramLookup = new HashMap<>();
                for (ParamInfo param : params) {
                    paramLookup.put(param.getName(), param);
                }
                List<JavadocBlockTag> blockTags = javadoc.getBlockTags();
                for (JavadocBlockTag tag : blockTags) {
                    if ("param".equals(tag.getTagName())) {
                        String paramName = tag.getName().get();
                        ParamInfo paramInfo = paramLookup.get(paramName);
                        if (null != paramInfo) {
                            paramInfo.setDescription(tag.getContent().toText());
                        }
                    }
                }
            }
        }
    }

    private static final Set<String> actionAnnotations = C.set("Action", "GetAction", "PostAction", "PutAction", "DeleteAction");

    private void exploreDeclaration(ClassOrInterfaceDeclaration classDeclaration, Map<String, Javadoc> methodJavaDocs, String prefix) {
        String className = classDeclaration.getName();
        String newPrefix = S.blank(prefix) ? className : S.concat(prefix, ".", className);
        for (Node node : classDeclaration.getChildrenNodes()) {
            if (node instanceof ClassOrInterfaceDeclaration) {
                exploreDeclaration((ClassOrInterfaceDeclaration) node, methodJavaDocs, newPrefix);
            } else if (node instanceof MethodDeclaration) {
                MethodDeclaration methodDeclaration = (MethodDeclaration) node;
                List<AnnotationExpr> annoList = methodDeclaration.getAnnotations();
                boolean needJavadoc = false;
                if (null != annoList && !annoList.isEmpty()) {
                    for (AnnotationExpr anno : annoList) {
                        String annoName = anno.getName().getName();
                        if (actionAnnotations.contains(annoName)) {
                            needJavadoc = true;
                            break;
                        }
                    }
                }
                if (!needJavadoc) {
                    continue;
                }
                Comment comment = methodDeclaration.getComment();
                if (!(comment instanceof JavadocComment)) {
                    continue;
                }
                JavadocComment javadocComment = (JavadocComment) comment;
                Javadoc javadoc = JavadocParser.parse(javadocComment);
                methodJavaDocs.put(S.concat(newPrefix, ".", methodDeclaration.getName()), javadoc);
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
