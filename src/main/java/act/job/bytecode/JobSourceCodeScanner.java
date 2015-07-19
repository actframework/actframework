package act.job.bytecode;

import act.app.AppSourceCodeScannerBase;
import act.app.Source;
import org.osgl.util.S;

import java.util.regex.Pattern;

/**
 * Scan {@link Source source code} to determine
 * controller classes, e.g. the class where action method is defined
 */
public class JobSourceCodeScanner extends AppSourceCodeScannerBase {

    private final Pattern PTN_ACTION_ANN = Pattern.compile(
            "(^|.*\\s+)@(AlongWith|Cron|Every|FixedDelay|InvokeAfter|InvokeBefore|OnAppStart|OnAppStop).*");

    @Override
    protected void _visit(int lineNumber, String line, String className) {
        if (S.blank(line)) return;
        if (PTN_ACTION_ANN.matcher(line).matches()) {
            markScanByteCode();
        }
    }

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

}
