package act.util;

import act.ActComponent;
import act.app.*;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.S;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@ActComponent
public abstract class SubTypeFinder extends AppCodeScannerPluginBase {

    private $.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler;
    private String pkgName;
    private String clsName;
    private Class<?> superType;
    private boolean publicOnly;
    private boolean noAbstract;

    protected SubTypeFinder(boolean publicOnly, boolean noAbstract, Class<?> superType, $.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler) {
        E.NPE(superType, foundHandler);
        this.clsName = superType.getSimpleName();
        this.pkgName = FastStr.of(superType.getName()).beforeLast('.').toString();
        this.superType = superType;
        this.foundHandler = foundHandler;
        this.noAbstract = noAbstract;
        this.publicOnly = publicOnly;
        logger.info("pkg: %s, cls: %s", pkgName, clsName);
    }
    protected SubTypeFinder(Class<?> superType, $.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler) {
        this(true, true, superType, foundHandler);
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

    @ActComponent
    private class SourceCodeSensor extends AppSourceCodeScannerBase {

        private boolean pkgFound;
        private final Pattern PATTERN = Pattern.compile(".*@Extends\\(\\s*" + clsName + "\\.class\\s*\\).*");

        @Override
        protected void reset(String className) {
            super.reset(className);
            pkgFound = false;
        }

        @Override
        protected void _visit(int lineNumber, String line, String className) {
            if (PATTERN.matcher(line).matches()) {
                markScanByteCode();
                logFound(className);
                return;
            }
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
            logger.info("Subtype of %s detected: %s", S.builder(pkgName).append(".").append(clsName), className);
        }

        @Override
        protected boolean shouldScan(String className) {
            return true;
        }

        @Override
        public int hashCode() {
            return $.hc(PATTERN, SourceCodeSensor.class);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof SourceCodeSensor) {
                SourceCodeSensor that = (SourceCodeSensor)obj;
                return $.eq(that.PATTERN, this.PATTERN);
            }
            return false;
        }
    }

    @ActComponent
    private class ByteCodeSensor extends AppByteCodeScannerBase {
        private ClassDetector detector;
        private $.Func2<App, String, Map<Class<? extends AppByteCodeScanner>, Set<String>>> foundHandler = SubTypeFinder.this.foundHandler;

        @Override
        protected void reset(String className) {
            super.reset(className);
            detector = ClassDetector.of(new DescendantClassFilter(publicOnly, noAbstract, superType) {
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
