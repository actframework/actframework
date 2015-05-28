package act.app;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import act.conf.AppConfig;
import act.controller.meta.ControllerClassMetaInfo;
import act.route.Router;
import act.util.Files;
import org.osgl.util.C;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dev mode application class loader, which is able to
 * load classes directly from app src folder
 */
public class DevModeClassLoader extends AppClassLoader {


    private Map<String, Source> sources = C.newMap();
    private final AppCompiler compiler;

    public DevModeClassLoader(App app) {
        super(app);
        compiler = new AppCompiler(this);
    }

    public boolean isSourceClass(String className) {
        return sources.containsKey(className);
    }

    public ControllerClassMetaInfo controllerClassMetaInfo(String controllerClassName) {
        return super.controllerClassMetaInfo(controllerClassName);
    }

    @Override
    protected void preload() {
        preloadSources();
        super.preload();
    }

    @Override
    protected void scan2() {
        super.scan2();
        scanSources();
    }

    @Override
    protected byte[] appBytecode(String name) {
        byte[] bytecode = super.appBytecode(name);
        return null == bytecode ? bytecodeFromSource(name) : bytecode;
    }

    public Source source(String className) {
        return sources.get(className);
    }

    private void preloadSources() {
        final File sourceRoot = app().layout().source(app().base());
        Files.filter(sourceRoot, App.F.JAVA_SOURCE, new _.Visitor<File>() {
            @Override
            public void visit(File file) throws _.Break {
                Source source = Source.ofFile(sourceRoot, file);
                if (null != source) {
                    if (null == sources) {
                        sources = C.newMap();
                    }
                    sources.put(source.className(), source);
                }
            }
        });
    }

    private void scanSources() {
        logger.debug("start to scan sources...");
        List<AppSourceCodeScanner> scanners = app().scannerManager().sourceCodeScanners();
        if (scanners.isEmpty()) {
            logger.warn("No source code scanner found");
            return;
        }

        Set<String> classesNeedByteCodeScan = C.newSet();
        for (String className : sources.keySet()) {
            logger.debug("scanning %s ...", className);
            List<AppSourceCodeScanner> l = C.newList();
            for (AppSourceCodeScanner scanner : scanners) {
                if (scanner.start(className)) {
                    logger.debug("scanner %s added to the list", scanner);
                    l.add(scanner);
                }
            }
            if (l.isEmpty()) {
                continue;
            }
            Source source = source(className);
            String[] lines = source.code().split("[\\n\\r]+");
            for (int i = 0, j = lines.length; i < j; ++i) {
                String line = lines[i];
                for (AppSourceCodeScanner scanner : l) {
                    scanner.visit(i, line, className);
                }
            }
            for (AppSourceCodeScanner scanner: l) {
                if (scanner.triggerBytecodeScanning()) {
                    logger.debug("bytecode scanning triggered on %s", className);
                    classesNeedByteCodeScan.add(className);
                    break;
                }
            }
        }

        if (classesNeedByteCodeScan.isEmpty()) {
            return;
        }

        scanByteCode(classesNeedByteCodeScan, new _.F1<String, byte[]>() {
            @Override
            public byte[] apply(String s) throws NotAppliedException, _.Break {
                return bytecodeFromSource(s);
            }
        });
    }



    private byte[] bytecodeFromSource(String name) {
        Source source = source(name);
        if (null == source) {
            return null;
        }
        byte[] bytes = source.bytes();
        if (null == bytes) {
            compiler.compile(name);
            bytes = source.bytes();
        }
        return bytes;
    }


}
