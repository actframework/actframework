package act.util;

import act.ActComponent;
import act.app.App;
import act.app.AppByteCodeScanner;
import act.app.AppByteCodeScannerBase;
import act.app.AppSourceCodeScanner;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.exception.NotAppliedException;
import org.osgl.util.C;
import org.osgl.util.E;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

@ActComponent
public abstract class AnnotatedTypeFinder extends AppCodeScannerPluginBase {

    private $.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler;
    private Class<? extends Annotation> annoType;
    private boolean noAbstract;
    private boolean publicOnly;
    private App app;
    protected Set<String> foundClasses = C.newSet();

    protected AnnotatedTypeFinder(boolean publicOnly, boolean noAbstract, Class<? extends Annotation> annoType, $.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler) {
        E.NPE(annoType);
        this.annoType = annoType;
        this.foundHandler = foundHandler;
        this.noAbstract = noAbstract;
        this.publicOnly = publicOnly;
    }

    protected AnnotatedTypeFinder(Class<? extends Annotation> annoType, $.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler) {
        this(true, true, annoType, foundHandler);
    }

    protected AnnotatedTypeFinder(Class<? extends Annotation> annoType) {
        this(true, true, annoType, null);
    }

    protected App app() {
        return app;
    }

    @Override
    public AppSourceCodeScanner createAppSourceCodeScanner(App app) {
        return null;
    }

    @Override
    public AppByteCodeScanner createAppByteCodeScanner(App app) {
        this.app = app;
        return new ByteCodeSensor();
    }

    @Override
    public boolean load() {
        return true;
    }

    @ActComponent
    private class ByteCodeSensor extends AppByteCodeScannerBase {
        private ClassDetector detector;
        private $.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler = AnnotatedTypeFinder.this.foundHandler;

        @Override
        protected void reset(String className) {
            super.reset(className);
            detector = ClassDetector.of(new AnnotatedClassFilter(publicOnly, noAbstract, annoType) {
                @Override
                public void found(Class clazz) {
                }
            });
        }

        @Override
        public ByteCodeVisitor byteCodeVisitor() {
            return detector;
        }

        @Override
        public void scanFinished(String className) {
            if (detector.found()) {
                foundClasses.add(className);
                Map<Class<? extends AppByteCodeScanner>, Set<String>> dependencies =
                        null == foundHandler ? null : foundHandler.apply(app(), className);
                if (null != dependencies && !dependencies.isEmpty()) {
                    for (Class<? extends AppByteCodeScanner> c : dependencies.keySet()) {
                        addDependencyClassToScanner(c, dependencies.get(c));
                    }
                }
            }
        }

        @Override
        protected boolean shouldScan(String className) {
            return true;
        }

        @Override
        public int hashCode() {
            return $.hc(detector, ByteCodeSensor.class);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof ByteCodeSensor) {
                ByteCodeSensor that = (ByteCodeSensor)obj;
                return $.eq(that.detector, this.detector) && $.eq(that.foundHandler, this.foundHandler);
            }
            return false;
        }
    }

}
