package act.data;

import org.osgl.util.IO;
import org.osgl.util.StringValueResolver;

/**
 * Read content as string from resource URL
 */
public class ContentStringResolver extends StringValueResolver<String> {

    public static final ContentStringResolver INSTANCE = new ContentStringResolver();

    @Override
    public String resolve(String value) {
        Boolean reportingError = attribute("reportError");
        if (null == reportingError) {
            reportingError = false;
        }
        if (reportingError) {
            return IO.readContentAsString(SObjectResolver.INSTANCE.resolve(value).asInputStream());
        } else {
            try {
                return IO.readContentAsString(SObjectResolver.INSTANCE.resolve(value).asInputStream());
            } catch (Exception e) {
                // ignore and return the value
                return value;
            }
        }
    }
}
