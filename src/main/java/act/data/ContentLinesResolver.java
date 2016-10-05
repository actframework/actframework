package act.data;

import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.StringValueResolver;

import java.util.List;

/**
 * Read content lines from resource URL
 */
public class ContentLinesResolver extends StringValueResolver<List<String>> {

    public static final ContentLinesResolver INSTANCE = new ContentLinesResolver();

    @Override
    public List<String> resolve(String value) {
        Boolean reportingError = attribute("reportError");
        if (null == reportingError) {
            reportingError = false;
        }
        if (reportingError) {
            return IO.readLines(SObjectResolver.INSTANCE.resolve(value).asInputStream());
        } else {
            try {
                return IO.readLines(SObjectResolver.INSTANCE.resolve(value).asInputStream());
            } catch (Exception e) {
                // ignore and return the value in list
                return C.list(value);
            }
        }
    }
}
