package act.app;

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

import static act.app.App.F.*;

import act.Act;
import act.controller.meta.ControllerClassMetaInfo;
import act.metric.Timer;
import act.util.*;
import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Dev mode application class loader, which is able to
 * load classes directly from app src folder
 */
public class DevModeClassLoader extends AppClassLoader {
    private final static Logger logger = L.get(DevModeClassLoader.class);

    private Map<String, Source> sources = new HashMap<>();
    private final AppCompiler compiler;

    private List<FsChangeDetector> detectors = new ArrayList<>();

    public DevModeClassLoader(App app) {
        super(app);
        compiler = new AppCompiler(this);
    }

    @Override
    protected void releaseResources() {
        sources.clear();
        compiler.destroy();
        super.releaseResources();
    }

    public boolean isSourceClass(String className) {
        if (sources.containsKey(className)) {
            return true;
        }
        Class<?> clazz = app().classForName(className);
        return null != source(clazz);
    }

    public Source source(Class<?> clazz) {
        String className = clazz.getName();
        Source source = sources.get(className);
        if (null != source) {
            return source;
        }
        Class<?> enclosingClass = clazz.getEnclosingClass();
        return null == enclosingClass ? null : source(enclosingClass);
    }

    public ControllerClassMetaInfo controllerClassMetaInfo(String controllerClassName) {
        return super.controllerClassMetaInfo(controllerClassName);
    }

    @Override
    public URL getResource(String name) {
        // TODO: handle multiple modules case
        File file = new File(RuntimeDirs.resource(app()), name);
        if (file.exists()) {
            return $.convert(file).to(URL.class);
        }
        URL url = super.getResource(name);
        if (null == url) {
            if (name.startsWith("/")) {
                name = name.substring(1);
                return super.getResource(name);
            }
        }
        return url;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        return null != url ? IO.inputStream(url) : super.getResourceAsStream(name);
    }

    @Override
    public InputStream getOriginalResourceAsStream(String name) {
        URL url = getOriginalResource(name);
        return null != url ? IO.inputStream(url) : super.getOriginalResourceAsStream(name);
    }

    @Override
    public URL getOriginalResource(String name) {
        File file = new File(RuntimeDirs.resource(app()), name);
        return file.exists() ? $.convert(file).to(URL.class) : super.getOriginalResource(name);
    }

    @Override
    protected void preload() {
        preloadSources();
        super.preload();
        setupFsChangeDetectors();
    }

    @Override
    protected void preloadClasses() {
        // do not preload classes in dev mode
    }

    @Override
    protected void scan() {
        compileSources();
        Set<String> sourcesToBeScanned = findSourcesToBeScanned();
        Set<String> libClasses = libClasses();
        if (Act.profile().equalsIgnoreCase("test")) {
            Set<String> toBeRemoved = new HashSet<>();
            for (String s : libClasses) {
                if (s.contains("$")) {
                    String s0 = S.cut(s).beforeFirst("$");
                    if (sourcesToBeScanned.contains(s0)) {
                        toBeRemoved.add(s);
                    }
                }
            }
            libClasses.removeAll(toBeRemoved);
        }
        libClasses.removeAll(sourcesToBeScanned);
        scan(libClasses);
        scanSources(sourcesToBeScanned);
    }

    @Override
    protected byte[] loadAppClassFromDisk(String name) {
        App app = app();
        List<File> srcRoots = app.sourceDirs();
        preloadSource(srcRoots, name);
        return bytecodeFromSource(name, true);
    }

    private void addSourceRoot(List<File> sourceRoots, File base, ProjectLayout layout) {
        if (null != base && base.isDirectory()) {
            sourceRoots.add(layout.source(base));
        }
    }

    @Override
    protected byte[] appBytecode(String name, boolean compileSource) {
        byte[] bytecode = super.appBytecode(name, compileSource);
        return null == bytecode && compileSource ? bytecodeFromSource(name, compileSource) : bytecode;
    }

    public Source source(String className) {
        if (className.contains("$")) {
            String name0 = S.before(className, "$");
            return sources.get(name0);
        }
        return sources.get(className);
    }

    private void preloadSources() {
        List<File> sourceRoots = app().allSourceDirs();
        for (final File sourceRoot : sourceRoots) {
            Files.filter(sourceRoot, JAVA_SOURCE, new $.Visitor<File>() {
                @Override
                public void visit(File file) throws $.Break {
                    Source source = Source.ofFile(sourceRoot, file);
                    if (null != source) {
                        if (null == sources) {
                            sources = new HashMap<>();
                        }
                        sources.put(source.className(), source);
                    }
                }
            });
        }
    }

    private void preloadSource(List<File> sourceRoot, String className) {
        if (null != sources) {
            Source source = sources.get(className);
            if (null != source) {
                return;
            }
        }
        Source source = Source.ofClass(sourceRoot, className);
        if (null != source) {
            if (null == sources) {
                sources = new HashMap<>();
            }
            sources.put(source.className(), source);
        }
    }

    private void compileSources() {
        long l = 0;
        if (logger.isDebugEnabled()) {
            logger.debug("Source compiling starts ...");
            l = $.ms();
        }
        Collection<Source> toBeCompiled = sources.values();
        compiler.compile(toBeCompiled);
        if (logger.isDebugEnabled()) {
            logger.debug("Source compiling takes %sms to compile %s sources", $.ms() - l, toBeCompiled.size());
        }
    }

    private Set<String> findSourcesToBeScanned() {
        Timer timer = metric.startTimer("act:classload:scan:findScanSources");
        try {
            logger.debug("start to scan sources...");
            List<AppSourceCodeScanner> scanners = app().scannerManager().sourceCodeScanners();

            Set<String> classesNeedByteCodeScan = C.newSet();
            if (scanners.isEmpty()) {
                //LOGGER.warn("No source code scanner found");
                for (String className : sources.keySet()) {
                    classesNeedByteCodeScan.add(className);
                }
            } else {
                for (String className : sources.keySet()) {
                    classesNeedByteCodeScan.add(className);
                    logger.trace("scanning %s ...", className);
                    List<AppSourceCodeScanner> l = new ArrayList<>();
                    for (AppSourceCodeScanner scanner : scanners) {
                        if (scanner.start(className)) {
                            //LOGGER.trace("scanner %s added to the list", scanner.getClass().getName());
                            l.add(scanner);
                        }
                    }
                    Source source = source(className);
                    String[] lines = source.code().split("[\\n\\r]+");
                    for (int i = 0, j = lines.length; i < j; ++i) {
                        String line = lines[i];
                        for (AppSourceCodeScanner scanner : l) {
                            scanner.visit(i, line, className);
                        }
                    }
                }
            }
            return classesNeedByteCodeScan;
        } finally {
            long ns = timer.ns();
            timer.stop();
            if (logger.isDebugEnabled()) {
                logger.debug("it takes %sms to find %s sources to be scanned and their bytecodes", ns / (1000 * 1000), sources.size());
            }
        }
    }

    private void scanSources(Set<String> classesNeedByteCodeScan) {
        Timer timer = metric.startTimer("act:classload:scan:scanSources");
        try {
            logger.debug("start to scan sources...");

            if (classesNeedByteCodeScan.isEmpty()) {
                return;
            }

            final Set<String> embeddedClassNames = C.newSet();
            scanByteCode(classesNeedByteCodeScan, new $.F1<String, byte[]>() {
                @Override
                public byte[] apply(String s) throws NotAppliedException, $.Break {
                    return bytecodeFromSource(s, embeddedClassNames);
                }
            });

            while (!embeddedClassNames.isEmpty()) {
                Set<String> embeddedClassNameCopy = C.newSet(embeddedClassNames);
                scanByteCode(embeddedClassNameCopy, new $.F1<String, byte[]>() {
                    @Override
                    public byte[] apply(String s) throws NotAppliedException, $.Break {
                        return bytecodeFromSource(s, embeddedClassNames);
                    }
                });
                embeddedClassNames.removeAll(embeddedClassNameCopy);
            }
        } finally {
            long ns = timer.ns();
            timer.stop();
            if (logger.isDebugEnabled()) {
                logger.debug("it takes %sms to scan %s sources and their bytecodes", ns / (1000 * 1000), sources.size());
            }
        }
    }

    private byte[] bytecodeFromSource(String name, boolean compile) {
        Source source = source(name);
        if (null == source) {
            return null;
        }
        byte[] bytes = source.bytes();
        if (null == bytes && compile) {
            compiler.compile(name);
            bytes = source.bytes();
        }
        if (name.contains("$")) {
            String innerClassName = S.afterFirst(name, "$");
            return source.bytes(innerClassName);
        }
        return bytes;
    }

    private byte[] bytecodeFromSource(String name, Set<String> embeddedClassNames) {
        Source source = source(name);
        if (null == source) {
            return null;
        }
        byte[] bytes = source.bytes();
        if (null == bytes) {
            compiler.compile(name);
            bytes = source.bytes();
        }
        if (!name.contains("$")) {
            embeddedClassNames.addAll(C.list(source.innerClassNames()).map(S.F.prepend(name + "$")));
        } else {
            String innerClassName = S.afterFirst(name, "$");
            return source.bytes(innerClassName);
        }
        return bytes;
    }

    @Override
    public void detectChanges() {
        // #1194 - prevent ConcurrentModificationException
        List<FsChangeDetector> detectors = new ArrayList<>(this.detectors);
        for (FsChangeDetector detector : detectors) {
            detectChanges(detector);
        }
        super.detectChanges();
    }

    public void registerResourceFileDetector(String resourcePath) {
        ProjectLayout layout = app().layout();
        addDetector(layout.resource(app().base()), S.F.endsWith(resourcePath), confChangeListener);
    }

    private void detectChanges(FsChangeDetector detector) {
        if (null != detector) {
            detector.detectChanges();
        }
    }

    private void setupFsChangeDetectors() {
        ProjectLayout layout = app().layout();
        File appBase = app().base();
        List<File> bases = C.newList(appBase);
        bases.addAll(app().config().moduleBases());
        boolean isTest = "test".equals(Act.profile());
        for (File base : bases) {
            addDetector(layout.source(base), JAVA_SOURCE, sourceChangeListener);
            addDetector(layout.lib(base), JAR_FILE, libChangeListener);
            File rsrc = layout.resource(base);
            addDetector(rsrc, CONF_FILE.or(ROUTES_FILE), confChangeListener);
            //addDetector(rsrc, null, resourceChangeListener);

            if (isTest) {
                addDetector(layout.testSource(base), JAVA_SOURCE, sourceChangeListener);
                addDetector(layout.testLib(base), JAR_FILE, libChangeListener);
                File testRsrc = layout.testResource(base);
                addDetector(testRsrc, CONF_FILE.or(ROUTES_FILE), confChangeListener);
                addDetector(testRsrc, null, resourceChangeListener);
            }
        }
    }

    private void addDetector(File base, $.Predicate<String> predicate, FsEventListener listener) {
        if (null != base && base.isDirectory()) {
            detectors.add(new FsChangeDetector(base, predicate, listener));
        }
    }

    private final FsEventListener sourceChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            throw Act.requestRefreshClassLoader();
        }
    };

    private final FsEventListener libChangeListener = new FsEventListener() {
        @Override
        public void on(FsEvent... events) {
            int len = events.length;
            if (len < 0) return;
            throw Act.requestRefreshClassLoader();
        }
    };

    private final FsEventListener confChangeListener = new DevModeClassLoader.ResourceChangeListener() {
        @Override
        public void on(FsEvent... events) {
            super.on(events);
            throw Act.requestRestart();
        }
    };

    private final FsEventListener resourceChangeListener = new ResourceChangeListener();

    private class ResourceChangeListener implements FsEventListener  {
        @Override
        public void on(FsEvent... events) {
//            int len = events.length;
//            for (int i = 0; i < len; ++i) {
//                FsEvent e = events[i];
//                List<String> paths = e.paths();
//                File[] files = new File[paths.size()];
//                int idx = 0;
//                for (String path : paths) {
//                    files[idx++] = new File(path);
//                }
//                switch (e.kind()) {
//                    case CREATE:
//                    case MODIFY:
//                        app().builder().copyResources(files);
//                        break;
//                    case DELETE:
//                        app().builder().removeResources(files);
//                        break;
//                    default:
//                        assert false;
//                }
//            }
        }
    }

}
