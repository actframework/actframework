package act.data;

import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.StringValueResolver;

import java.util.List;

import static act.data.annotation.ReadContent.ATTR_MERCY;

/**
 * Read content lines from resource URL
 */
public class ContentLinesResolver extends StringValueResolver<List<String>> {

    public static final ContentLinesResolver INSTANCE = new ContentLinesResolver();

    @Override
    public List<String> resolve(String value) {
        try {
            ISObject sobj = SObjectResolver.INSTANCE.resolve(value);
            return null == sobj ? fallBack(value) : IO.readLines(sobj.asInputStream());
        } catch (Exception e) {
            return fallBack(value);
        }
    }

    private List<String> fallBack(String value) {
        Boolean mercy = attribute(ATTR_MERCY);
        if (null == mercy) {
            mercy = false;
        }
        return mercy ? C.list(value) : C.<String>list();
    }

}
