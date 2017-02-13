package act.util;

import act.app.App;
import act.app.event.AppEventId;
import act.sys.Env;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.logging.Logger;
import org.osgl.util.S;

public class ClassFinderData {

    private static final Logger logger = App.logger;

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
                public void visit(ClassNode classNode) throws Osgl.Break {
                    ClassLoader cl = app.classLoader();
                    if (data.className.startsWith("act.")) {
                        cl = cl.getParent();
                    }
                    Class<?> targetClass = $.classForName(classNode.name(), app.classLoader());
                    if (!Env.matches(targetClass)) {
                        logger.debug("ignore target class[%s]: environment spec not matching", targetClass);
                        return;
                    }
                    if (data.isStatic) {
                        Class<?> host = $.classForName(data.className, cl);
                        $.invokeStatic(host, data.methodName, targetClass);
                    } else {
                        Object host = app.getInstance(data.className);
                        $.invokeVirtual(host, data.methodName, targetClass);
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
    }

    /**
     * The name of the class used to find the target classes
     */
    private String what;
    /**
     * Define when to invoke the found logic
     */
    private AppEventId when = AppEventId.DEPENDENCY_INJECTOR_PROVISIONED;
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
     * The name of the method that defines the found callback logic
     */
    private String methodName;
    /**
     * Is the found callback logic defined in static or instance method
     */
    private boolean isStatic;

    public ClassFinderData what(String targetClassName) {
        this.what = targetClassName;
        return this;
    }

    public ClassFinderData publicOnly(boolean b) {
        this.publicOnly = b;
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
        this.when = AppEventId.valueOf(loadOn);
        return this;
    }

    public ClassFinderData how(By by) {
        this.how = by;
        return this;
    }

    public ClassFinderData callback(String className, String methodName, boolean isStatic) {
        this.className = className;
        this.methodName = methodName;
        this.isStatic = isStatic;
        return this;
    }

    public boolean isValid() {
        return S.noBlank(className, methodName);
    }

    public void scheduleFind() {
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
