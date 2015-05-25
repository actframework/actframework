package act.di.guice;

import act.app.AppByteCodeScannerBase;
import com.google.inject.AbstractModule;
import org.osgl._;
import act.di.DependencyInjector;
import act.util.ByteCodeVisitor;
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
                logger.info("Guice injector added to app");
                app().injector(injector);
            }
            GuiceDependencyInjector guiceInjector = (GuiceDependencyInjector)injector;
            Class<? extends AbstractModule> c = _.classForName(className, app().classLoader());
            AbstractModule module = _.newInstance(c);
            guiceInjector.addModule(module);
            logger.info("guice module %s added to the injector", className);
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
                logger.info("guice module found: %s", name);
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }
}
