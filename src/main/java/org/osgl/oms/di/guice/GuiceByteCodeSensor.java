package org.osgl.oms.di.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.osgl._;
import org.osgl.oms.app.AppByteCodeScannerBase;
import org.osgl.oms.di.DependencyInjector;
import org.osgl.oms.util.ByteCodeVisitor;
import org.osgl.util.S;

class GuiceByteCodeSensor extends AppByteCodeScannerBase {

    private boolean isGuiceModule = false;

    @Override
    protected void reset(String className) {
        isGuiceModule = false;
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new _ByteCodeVisitor();
    }

    @Override
    public void scanFinished(String className) {
        if (isGuiceModule) {
            DependencyInjector injector = app().injector();
            if (null == injector) {
                injector = new GuiceDependencyInjector();
                app().injector(injector);
            }
            if (injector instanceof GuiceDependencyInjector) {
                GuiceDependencyInjector guiceInjector = (GuiceDependencyInjector)injector;
                Class<? extends AbstractModule> c = _.classForName(className, app().classLoader());
                AbstractModule module = _.newInstance(c);
                guiceInjector.addModule(module);
            }
        }
    }

    @Override
    public void allScanFinished() {

    }

    private class _ByteCodeVisitor extends ByteCodeVisitor {
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (S.eq("com/google/inject/AbstractModule", superName)) {
                isGuiceModule = true;
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }
}
