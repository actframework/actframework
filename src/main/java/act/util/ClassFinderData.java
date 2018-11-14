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

import act.app.App;
import act.app.event.SysEventId;
import act.sys.Env;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;

public class ClassFinderData {

    private static final Logger logger = LogManager.get(ClassFinderData.class);

    public enum By {
        ANNOTATION() {
            @Override
            void tryFind(
                    ClassNode startNode,
                    boolean publicOnly,
                    boolean noAbstract,
                    $.Visitor<ClassNode> visitor
            ) {
                startNode.visitAnnotatedClasses(visitor, publicOnly, noAbstract);
            }

            @Override
            String toString(ClassFinderData finder) {
                return "finding annotated by " + finder.what +
                        " on " + finder.when +
                        " for " + finder.className +
                        "::" + finder.methodName;
            }
        },

        SUPER_TYPE() {
            @Override
            void tryFind(
                    ClassNode startNode,
                    boolean publicOnly,
                    boolean noAbstract,
                    $.Visitor<ClassNode> visitor
            ) {
                startNode.visitSubTree(visitor, publicOnly, noAbstract);
            }

            @Override
            String toString(ClassFinderData finder) {
                return "finding sub type of " + finder.what +
                        " on " + finder.when +
                        " for " + finder.className +
                        "::" + finder.methodName;
            }
        };

        public void find(final App app, final ClassFinderData data) {
            ClassInfoRepository repo = app.classLoader().classInfoRepository();
            final ClassNode theNode = repo.node(data.what);
            if (null == theNode) {
                logger.error("Cannot locate the \"what\" class[%s] in class info repository", data.what);
                return;
            }
            $.Visitor<ClassNode> visitor = new $.Visitor<ClassNode>() {
                @Override
                public void visit(ClassNode classNode) throws $.Break {
                    ClassLoader cl = app.classLoader();

                    if (data.className.startsWith("act.")) {
                        cl = cl.getParent();
                    }
                    Class<?> targetClass = app.classForName(classNode.name());
                    if (targetClass.isAnnotationPresent(NoAutoRegister.class)) {
                        return;
                    }
                    Object param = targetClass;
                    if (data.paramIsInstance) {
                        param = app.getInstance(targetClass);
                    }
                    if (!Env.matches(targetClass)) {
                        logger.debug("ignore target class[%s]: environment spec not matching", targetClass);
                        return;
                    }
                    if (data.isStatic) {
                        Class<?> host = $.classForName(data.className, cl);
                        $.invokeStatic(host, data.methodName, param);
                    } else {
                        Object host = app.getInstance(data.className);
                        $.invokeVirtual(host, data.methodName, param);
                    }
                }
            };
            tryFind(theNode, data.publicOnly, data.noAbstract, visitor);
        }

        abstract void tryFind(
                ClassNode startNode,
                boolean publicOnly,
                boolean noAbstract,
                $.Visitor<ClassNode> visitor);
        abstract String toString(ClassFinderData finder);
    }

    /**
     * The name of the class used to find the target classes
     */
    private String what;
    /**
     * Define when to invoke the found logic
     */
    private SysEventId when = SysEventId.DEPENDENCY_INJECTOR_PROVISIONED;
    /**
     * Specify how to find the target classes by matching
     * the `what` class
     */
    private By how;
    /**
     * Only scan public class?
     */
    private boolean publicOnly = true;
    /**
     * Do not scan abstract class?
     */
    private boolean noAbstract = true;
    /**
     * The name of the class that host the found callback logic
     */
    private String className;
    /**
     * Report if param is an instance of a class
     */
    private boolean paramIsInstance;
    /**
     * The name of the method that defines the found callback logic
     */
    private String methodName;
    /**
     * Is the found callback logic defined in static or instance method
     */
    private boolean isStatic;

    @Override
    public String toString() {
        return how.toString(this);
    }

    public ClassFinderData what(String targetClassName) {
        this.what = targetClassName;
        return this;
    }

    public ClassFinderData publicOnly(boolean b) {
        this.publicOnly = b;
        return this;
    }

    public ClassFinderData paramIsInstance(boolean b) {
        this.paramIsInstance = b;
        return this;
    }

    public ClassFinderData noAbstract(boolean b) {
        this.noAbstract = b;
        return this;
    }

    public boolean whatSpecified() {
        return S.notBlank(this.what) && S.neq(SubClassFinder.DEF_VALUE, this.what);
    }

    public ClassFinderData when(String loadOn) {
        this.when = SysEventId.valueOf(loadOn);
        return this;
    }

    public ClassFinderData how(By by) {
        this.how = by;
        return this;
    }

    public ClassFinderData callback(String className, String methodName, boolean isStatic, boolean paramIsInstance) {
        this.className = className;
        this.methodName = methodName;
        this.isStatic = isStatic;
        this.paramIsInstance = paramIsInstance;
        return this;
    }

    public boolean isValid() {
        return S.noBlank(className, methodName);
    }

    public void scheduleFind() {
        if (logger.isTraceEnabled()) {
            logger.trace("schedule class finding for %s", this);
        }
        final App app = App.instance();
        app.jobManager().on(when, jobId(), new Runnable() {
            @Override
            public void run() {
                how.find(app, ClassFinderData.this);
            }
        });
    }

    private String jobId() {
        return S.fmt("ClassFinder[%s@%s.%s]", what, className, methodName);
    }
}
