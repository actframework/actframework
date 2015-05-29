package act.app;

/**
 * Base class to implement the {@link AppSourceCodeScanner}
 */
public abstract class AppSourceCodeScannerBase extends AppCodeScannerBase implements AppSourceCodeScanner {
    private boolean scanByteCode = false;

    protected final void reset() {
        scanByteCode = false;
    }

    @Override
    public void visit(int lineNumber, String line, String className) {
        if (scanByteCode) return;
        _visit(lineNumber, line, className);
    }

    @Override
    public boolean triggerBytecodeScanning() {
        return scanByteCode;
    }

    protected void markScanByteCode() {
        scanByteCode = true;
    }

    protected abstract void _visit(int lineNumber, String line, String className);

    protected abstract boolean shouldScan(String className);
}
