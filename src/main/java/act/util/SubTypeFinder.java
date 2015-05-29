package act.util;

import act.app.*;
import org.osgl._;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.S;

import java.util.regex.Pattern;

public abstract class SubTypeFinder extends AppCodeScannerPluginBase {

    private _.Func2<App, String, ?> foundHandler;
    private String pkgName;
    private String clsName;
    private Class superType;

    protected SubTypeFinder(Class<?> superType, _.Func2<App, String, ?> foundHandler) {
        E.NPE(superType, foundHandler);
        this.clsName = superType.getSimpleName();
        this.pkgName = FastStr.of(superType.getName()).beforeLast('.').toString();
        this.superType = superType;
        this.foundHandler = foundHandler;
        logger.info("pkg: %s, cls: %s", pkgName, clsName);
    }

    @Override
    public AppSourceCodeScanner createAppSourceCodeScanner(App app) {
        return new SourceCodeSensor();
    }

    @Override
    public AppByteCodeScanner createAppByteCodeScanner(App app) {
        return new ByteCodeSensor();
    }

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
            return _.hc(PATTERN, SourceCodeSensor.class);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof SourceCodeSensor) {
                SourceCodeSensor that = (SourceCodeSensor)obj;
                return _.eq(that.PATTERN, this.PATTERN);
            }
            return false;
        }
    }

    private class ByteCodeSensor extends AppByteCodeScannerBase {
        private ClassDetector detector;
        private _.Func2<App, String, ?> foundHandler = SubTypeFinder.this.foundHandler;

        @Override
        protected void reset(String className) {
            super.reset(className);
            detector = ClassDetector.of(new ClassFilter() {
                @Override
                public void found(Class clazz) {
                }

                @Override
                public Class superType() {
                    return superType;
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
                foundHandler.apply(app(), className);
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
