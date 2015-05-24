package act.controller;

import act.app.AppSourceCodeScannerBase;
import act.app.Source;
import org.osgl.util.S;

import java.util.regex.Pattern;

/**
 * Scan {@link Source source code} to determine
 * controller classes, e.g. the class where action method is defined
 */
public class ControllerSourceCodeScanner extends AppSourceCodeScannerBase {

    private final Pattern PTN_ACTION_ANN = Pattern.compile(
            "(^|.*\\s+)@(Action|GetAction|PostAction|DeleteAction|PutAction).*");

    @Override
    protected void _visit(int lineNumber, String line, String className) {
        if (S.blank(line)) return;
        if (PTN_ACTION_ANN.matcher(line).matches()) {
            markScanByteCode();
        }
    }

    @Override
    protected boolean shouldScan(String className) {
        return config().possibleControllerClass(className);
    }

}
