package act.app;

import act.ActComponent;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;

import java.util.List;

/**
 * Manage {@link AppSourceCodeScanner} and {@link AppByteCodeScanner}
 * for the application
 */
@ActComponent
public class AppCodeScannerManager extends AppServiceBase<AppCodeScannerManager> {

    private static final Logger logger = L.get(AppCodeScannerManager.class);

    private C.List<AppSourceCodeScanner> sourceCodeScanners = C.newList();
    private C.List<AppByteCodeScanner> byteCodeScanners = C.newList();

    public AppCodeScannerManager(App app) {
        super(app);
    }

    public C.List<AppSourceCodeScanner> sourceCodeScanners() {
        return C.list(sourceCodeScanners);
    }

    public C.List<AppByteCodeScanner> byteCodeScanners() {
        return C.list(byteCodeScanners);
    }

    public AppByteCodeScanner byteCodeScannerByClass(Class<? extends AppByteCodeScanner> c) {
        for (AppByteCodeScanner scanner : byteCodeScanners) {
            if (scanner.getClass() == c) {
                return scanner;
            }
        }
        return null;
    }

    public AppCodeScannerManager register(AppSourceCodeScanner sourceCodeScanner) {
        _register(sourceCodeScanner, sourceCodeScanners);
        return this;
    }

    public AppCodeScannerManager register(AppByteCodeScanner byteCodeScanner) {
        _register(byteCodeScanner, byteCodeScanners);
        return this;
    }

    @Override
    protected void releaseResources() {
        sourceCodeScanners.clear();
        byteCodeScanners.clear();
    }

    private <T extends AppCodeScanner> void _register(T scanner, List<T> scanners) {
        scanner.setApp(app());
        if (scanners.contains(scanner)) {
            logger.warn("%s has already been registered", scanner);
            return;
        }
        scanners.add(scanner);
    }

}
