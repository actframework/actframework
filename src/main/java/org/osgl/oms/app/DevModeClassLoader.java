package org.osgl.oms.app;

import org.osgl._;
import org.osgl.exception.NotAppliedException;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.controller.meta.ControllerClassMetaInfo;
import org.osgl.oms.route.Router;
import org.osgl.oms.util.Files;
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
        ControllerClassMetaInfo info = super.controllerClassMetaInfo(controllerClassName);
        if (null != info) {
            return info;
        }
        Source source = source(controllerClassName);
        if (null != source && source.isController()) {
            return controllerInfo.scanForControllerMetaInfo(controllerClassName);
        }
        return null;
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
    @Deprecated
    protected void scan() {
        preloadSources();
        super.scan();
    }

    private void _scanForActionMethods() {
        AppConfig conf = app().config();
        SourceCodeActionScanner scanner = new SourceCodeActionScanner();
        Router router = app().router();
        for (String className : sources.keySet()) {
            if (conf.possibleControllerClass(className)) {
                Source source = sources.get(className);
                boolean isController = scanner.scan(className, source.code(), router);
                if (isController) {
                    source.markAsController();
                    scanForActionMethods(className);
                }
            }
        }
    }

    @Override
    protected void scanForActionMethods() {
        _scanForActionMethods();
        super.scanForActionMethods();
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
        List<AppSourceCodeScanner> scanners = app().scannerManager().sourceCodeScanners();
        if (scanners.isEmpty()) {
            logger.warn("No source code scanner found");
            return;
        }

        Set<String> classesNeedByteCodeScan = C.newSet();
        for (String className : sources.keySet()) {
            List<AppSourceCodeScanner> l = C.newList();
            for (AppSourceCodeScanner scanner : scanners) {
                if (scanner.start(className)) {
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
