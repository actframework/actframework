package act.mail;

import act.app.AppSourceCodeScannerBase;
import act.app.Source;
import org.osgl.util.S;

import java.util.regex.Pattern;

/**
 * Scan {@link Source source code} to determine
 * mailer classes, e.g. the class where sender method is defined
 */
public class MailerSourceCodeScanner extends AppSourceCodeScannerBase {

    private final Pattern PTN_MAILER_ANN = Pattern.compile(
            "(^|.*\\s+)@(Mailer).*");

    @Override
    protected void _visit(int lineNumber, String line, String className) {
        if (S.blank(line)) return;
        if (PTN_MAILER_ANN.matcher(line).matches()) {
            markScanByteCode();
        }
    }

    @Override
    protected boolean shouldScan(String className) {
        return config().possibleControllerClass(className);
    }

}
