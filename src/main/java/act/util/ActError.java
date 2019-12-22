package act.util;

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

import act.Act;
import act.app.*;
import act.asm.AsmContext;
import act.cli.CliContext;
import act.handler.DelegateRequestHandler;
import act.handler.RequestHandler;
import act.handler.builtin.controller.ControllerAction;
import act.handler.builtin.controller.RequestHandlerProxy;
import act.handler.builtin.controller.impl.ReflectedHandlerInvoker;
import act.i18n.I18n;
import org.osgl.$;
import org.osgl.http.H;
import org.osgl.mvc.MvcConfig;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public interface ActError {
    Throwable getCauseOrThis();
    Throwable getCause();
    SourceInfo sourceInfo();
    List<String> stackTrace();
    String getMessage();
    String getLocalizedMessage();
    boolean isErrorSpot(String traceLine, String nextTraceLine);

    class Util {

        public static String errorMessage(H.Status status) {
            return errorMessage(status, null);
        }

        public static String errorMessage(H.Status status, String message, Object ... args) {
            if (S.notBlank(message)) {
                return S.fmt(message, args);
            }
            if (Act.isProd()) {
                return MvcConfig.errorMessage(status);
            }
            ActionContext ctx = ActionContext.current();
            if (null == ctx) {
                return MvcConfig.errorMessage(status);
            }
            RequestHandler handler = ctx.handler();
            if (null == handler) {
                return MvcConfig.errorMessage(status);
            }
            if (H.Status.NOT_FOUND == status) {
                return I18n.i18n(Act.class, "e404.null_value_returned", handler);
            }
            return I18n.i18n(Act.class, "error.on_invoking", MvcConfig.errorMessage(status), handler);
        }

        public static SourceInfo loadSourceInfo(Throwable cause, Class<? extends ActError> errorClass) {
            if (cause instanceof SourceInfo) {
                return (SourceInfo) cause;
            }
            try {
                return _loadSourceInfo(cause.getStackTrace(), errorClass);
            } catch (RuntimeException e) {
                Act.LOGGER.warn(e, "Error loading source info");
                return null;
            }
        }

        private static SourceInfo _loadSourceInfo(StackTraceElement[] sa, Class<? extends ActError> errorClass) {
            SourceInfo info = loadSourceInfo(sa, errorClass);
            if (null != info) {
                return info;
            }
            int len = sa.length;
            StackTraceElement[] caller = sa;
            if (len > 0) {
                caller = new StackTraceElement[len - 1];
                System.arraycopy(sa, 1, caller, 0, len - 1);
            }

            ActContext ctx = ActContext.Base.currentContext();
            if (null == ctx) {
                return loadSourceInfo(caller, errorClass);
            }
            if (ctx instanceof ActionContext) {
                ActionContext actionContext = $.cast(ctx);
                RequestHandler handler = actionContext.handler();
                String actionName = handler.toString();
                for (StackTraceElement element : caller) {
                    if (actionName.contains(element.getMethodName()) && actionName.contains(element.getClassName())) {
                        return loadSourceInfo(caller, errorClass);
                    }
                }
                if (handler instanceof DelegateRequestHandler) {
                    handler = ((DelegateRequestHandler) handler).realHandler();
                }
                if (handler instanceof RequestHandlerProxy) {
                    RequestHandlerProxy proxy = $.cast(handler);
                    try {
                        ControllerAction action = proxy.actionHandler();
                        ReflectedHandlerInvoker invoker = $.cast(action.invoker());
                        return loadSourceInfo(invoker.method());
                    } catch (RuntimeException e) {
                        // refer: https://github.com/actframework/actframework/issues/1264
                        // try my best to get the method
                        String methodName = S.cut(actionName).afterLast(".");
                        String className = S.cut(actionName).beforeLast(".");
                        Class<?> clz = Act.classForName(className);
                        for (Method m : clz.getMethods()) {
                            if (methodName.equals(m.getName())) {
                                return loadSourceInfo(m);
                            }
                        }
                        for (Method m : clz.getDeclaredMethods()) {
                            if (methodName.equals(m.getName())) {
                                return loadSourceInfo(m);
                            }
                        }
                    }
                }
                return loadSourceInfo(caller, errorClass);
            } else if (ctx instanceof CliContext) {
                CliContext cliContext = $.cast(ctx);
                Method method = cliContext.handlerMethod();
                for (StackTraceElement element : caller) {
                    if (method.getName().equals(element.getMethodName()) && method.getDeclaringClass().getName().equals(element.getClassName())) {
                        return loadSourceInfo(caller, errorClass);
                    }
                }
                return loadSourceInfo(method);
            }
            return loadSourceInfo(caller, errorClass);
        }

        public static SourceInfo loadSourceInfo(Class<? extends ActError> errorClass) {
            return loadSourceInfo(new RuntimeException(), errorClass);
        }

        public static List<String> stackTraceOf(ActError error) {
            Throwable cause = error.getCause();
            ActError root = error;
            if (null == cause) {
                cause = (Throwable) error;
                root = null;
            }
            return stackTraceOf(cause, root);
        }

        public static List<String> stackTraceOf(Throwable t, ActError root) {
            List<String> l = new ArrayList<>();
            while (null != t) {
                StackTraceElement[] a = t.getStackTrace();
                for (StackTraceElement e : a) {
                    String line = S.concat("at ", e.toString());
                    if (line.contains("org.osgl.util.E.")) {
                        // skip E util class
                        continue;
                    }
                    if (l.contains(line)) {
                        l.add(line);
                        // caused by stack trace stop at here
                        break;
                    }
                    l.add(line);
                }
                t = t.getCause();
                if (t == root) {
                    break;
                }
                if (null != t) {
                    l.add("Caused by " + t.toString());
                }
            }
            return l;
        }

        public static SourceInfo loadSourceInfo(StackTraceElement[] stackTraceElements, Class<? extends ActError> errClz) {
            E.illegalStateIf(Act.isProd());
            DevModeClassLoader cl = (DevModeClassLoader) App.instance().classLoader();
            String errClzName = errClz.getName();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                int line = stackTraceElement.getLineNumber();
                if (line <= 0) {
                    continue;
                }
                String className = stackTraceElement.getClassName();
                if (S.eq(className, errClzName)) {
                    continue;
                }
                Source source = cl.source(className);
                if (null == source) {
                    continue;
                }
                return new SourceInfoImpl(source, line);
            }
            return null;
        }

        public static SourceInfo loadSourceInfo(Method method) {
            return loadSourceInfo(method.getDeclaringClass().getName(), method.getName(), true, null);
        }

        public static SourceInfo loadSourceInfo(AsmContext asmContext) {
            return loadSourceInfo(asmContext.className(), asmContext.name(), ElementType.METHOD == asmContext.type(), asmContext.lineNo());
        }

        private static SourceInfo loadSourceInfo(String className, String elementName, boolean isMethod, Integer lineNo) {
            E.illegalStateIf(Act.isProd());
            DevModeClassLoader cl = (DevModeClassLoader) App.instance().classLoader();
            Source source = cl.source(className);
            if (null == source) {
                return null;
            }
            List<String> lines = source.lines();
            Line candidate = null;
            String pattern = isMethod ?
                    S.concat("^\\s*.*", elementName, "\\s*\\(.*") :
                    S.concat("^\\s*.*", elementName, "[^\\(\\{]*");
            if (null != lineNo) {
                return new SourceInfoImpl(source, lineNo);
            }
            for (int i = 0; i < lines.size(); ++i) {
                String line = lines.get(i);
                if (line.matches(pattern)) {
                    candidate = new Line(line, i + 1);
                    if (candidate.forSure) {
                        return new SourceInfoImpl(source, candidate.no);
                    }
                }
            }
            if (null != candidate) {
                return new SourceInfoImpl(source, candidate.no);
            }
            return new SourceInfoImpl(source, 1);
        }

        private static class Line {
            String line;
            int no;
            boolean forSure;
            Line(String line, int no) {
                this.line = line;
                this.no = no;
                forSure = line.contains("public ");
            }
        }
    }
}
