package act.inject.genie;

import act.app.AppByteCodeScannerBase;
import act.app.event.AppEventId;
import act.util.ByteCodeVisitor;
import org.osgl.$;

/**
 * Find all classes that ends with `Module`, try to register it as
 * Genie module
 */
public class GenieModuleScanner extends AppByteCodeScannerBase {

    private boolean shouldRegister;

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new ByteCodeVisitor() {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                shouldRegister = isPublic(access);
            }
        };
    }

    @Override
    public void scanFinished(final String className) {
        if (shouldRegister) {
            app().jobManager().on(AppEventId.DEPENDENCY_INJECTOR_LOADED, new Runnable() {
                @Override
                public void run() {
                    GenieInjector.addModuleClass($.classForName(className, app().classLoader()));
                }
            });
        }
    }

    @Override
    protected boolean shouldScan(final String className) {
        return className.endsWith("Module");
    }
}
