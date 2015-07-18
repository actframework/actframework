package act.util;

import act.app.AppSourceCodeScannerBase;

public class ClassInfoSourceCodeScanner extends AppSourceCodeScannerBase {
    @Override
    protected void _visit(int lineNumber, String line, String className) {
        markScanByteCode();
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }
}
