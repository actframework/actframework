package act.util;

import act.app.*;
import org.osgl._;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class AnnotatedTypeFinder extends AppCodeScannerPluginBase {

    private _.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler;
    private String clsName;
    private String pkgName;
    private Class<? extends Annotation> annoType;

    protected AnnotatedTypeFinder(Class<? extends Annotation> annoType, _.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler) {
        E.NPE(annoType, foundHandler);
        this.clsName = annoType.getSimpleName();
        this.pkgName = FastStr.of(annoType.getName()).beforeLast('.').toString();
        this.annoType = annoType;
        this.foundHandler = foundHandler;
    }

    @Override
    public AppSourceCodeScanner createAppSourceCodeScanner(App app) {
        return new SourceCodeSensor();
    }

    @Override
    public AppByteCodeScanner createAppByteCodeScanner(App app) {
        return new ByteCodeSensor();
    }

    @Override
    public boolean load() {
        return true;
    }

    private class SourceCodeSensor extends AppSourceCodeScannerBase {

        private boolean pkgFound;

        @Override
        protected void reset(String className) {
            super.reset(className);
            pkgFound = false;
        }

        @Override
        protected void _visit(int lineNumber, String line, String className) {
            if (!pkgFound) {
                if (line.contains(pkgName)) {
                    pkgFound = true;
                }
            }
            if (pkgFound) {
                boolean found = line.contains(clsName);
                if (found) {
                    markScanByteCode();
                    logFound(className);
                }
            }
        }

        protected void logFound(String className) {
            logger.info("[%s]annotated type detected: %s", S.builder(pkgName).append(".").append(clsName), className);
        }

        @Override
        protected boolean shouldScan(String className) {
            return true;
        }

        private String key() {
            return pkgName + clsName;
        }

        @Override
        public int hashCode() {
            return _.hc(pkgName, clsName, SourceCodeSensor.class);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof SourceCodeSensor) {
                SourceCodeSensor that = (SourceCodeSensor)obj;
                return _.eq(that.key(), this.key());
            }
            return false;
        }
    }

    private class ByteCodeSensor extends AppByteCodeScannerBase {
        private ClassDetector detector;
        private _.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler = AnnotatedTypeFinder.this.foundHandler;

        @Override
        protected void reset(String className) {
            super.reset(className);
            detector = ClassDetector.of(new AnnotatedClassFilter(annoType) {
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
                Map<Class<? extends AppByteCodeScanner>, Set<String>> dependencies = foundHandler.apply(app(), className);
                if (null != dependencies && !dependencies.isEmpty()) {
                    for (Class<? extends AppByteCodeScanner> c : dependencies.keySet()) {
                        addDependencyClassToScanner(c, dependencies.get(c));
                    }
                }
            }
        }

        @Override
        public void allScanFinished() {
        }

        @Override
        protected boolean shouldScan(String className) {
            return true;
        }

        @Override
        public int hashCode() {
            return _.hc(detector, ByteCodeSensor.class);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof ByteCodeSensor) {
                ByteCodeSensor that = (ByteCodeSensor)obj;
                return _.eq(that.detector, this.detector) && _.eq(that.foundHandler, this.foundHandler);
            }
            return false;
        }
    }

}
