package org.osgl.oms.app;

import org.osgl._;
import org.osgl.oms.OMS;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.controller.Controller;
import org.osgl.oms.controller.meta.ControllerClassMetaInfo;
import org.osgl.oms.route.Router;
import org.osgl.oms.util.Files;
import org.osgl.oms.util.FsChangeDetector;
import org.osgl.oms.util.FsEvent;
import org.osgl.oms.util.FsEventListener;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Dev mode application class loader, which is able to
 * load classes directly from app srccode folder
 */
public class DevModeClassLoader extends AppClassLoader {


    private final AppCompiler compiler;

    public DevModeClassLoader(App app) {
        super(app);
        compiler = new AppCompiler(this);
    }

    @Override
    public boolean isSourceClass(String className) {
        return app().isSourceClass(className);
    }

    public ControllerClassMetaInfo controllerClassMetaInfo(String controllerClassName) {
        ControllerClassMetaInfo info = super.controllerClassMetaInfo(controllerClassName);
        if (null != info) {
            return info;
        }
        Source source = source(controllerClassName);
        if (null != source && source.isController()) {
            return ctrlInfo.scanForControllerMetaInfo(controllerClassName);
        }
        return null;
    }

    @Override
    protected byte[] lookupByte(String className) {
        byte[] ba = super.lookupByte(className);
        if (null == ba) {
            Source source = app().source(className);
            if (null != source && source.isController()) {
                return bytecodeFromSource(className);
            }
        }
        return ba;
    }

    @Override
    protected void scan() {
        app().preloadSources();
        super.scan();
    }

    @Override
    protected void scanForActionMethods() {
        app().scanForActionMethods();
        super.scanForActionMethods();
    }

    @Override
    protected byte[] appBytecode(String name) {
        byte[] bytecode = super.appBytecode(name);
        return null == bytecode ? bytecodeFromSource(name) : bytecode;
    }

    Source source(String name) {
        return app().source(name);
    }


    private byte[] bytecodeFromSource(String name) {
        Source source = app().source(name);
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
