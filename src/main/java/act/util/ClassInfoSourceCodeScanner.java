package act.util;

import act.app.AppByteCodeScannerBase;
import act.app.AppSourceCodeScannerBase;
import act.app.event.AppEventId;
import act.asm.Type;
import act.event.ActEvent;
import act.event.AppEventListenerBase;

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
