package act.util;

import act.app.App;
import act.app.event.AppEventId;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.logging.Logger;
import org.osgl.util.S;

public class ClassFinderData {

    private static final Logger logger = App.logger;

    public enum By {
        ANNOTATION() {
            @Override
            void tryFind(ClassNode startNode, $.Visitor<ClassNode> visitor) {
                startNode.visitPublicAnnotatedClasses(visitor);
            }
        },

        SUPER_TYPE() {
            @Override
            void tryFind(ClassNode startNode, $.Visitor<ClassNode> visitor) {
                startNode.visitPublicSubTreeNodes(visitor);
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
                    Class<?> targetClass = $.classForName(classNode.name(), app.classLoader());
                    if (data.isStatic) {
                        Class<?> host = $.classForName(data.className, app.classLoader());
                        $.invokeStaticMethod(host, data.methodName, targetClass);
                    } else {
                        Object host = app.newInstance(data.className);
                        $.invokeInstanceMethod(host, data.methodName, targetClass);
                    }
                }
            };
            tryFind(theNode, visitor);
        }

        abstract void tryFind(final ClassNode startNode, final $.Visitor<ClassNode> visitor);
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
