package act.data;

import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.StringValueResolver;

import java.util.List;

import static act.data.annotation.ReadContent.ATTR_MERCY;

/**
 * Read content as string from resource URL
 */
public class ContentStringResolver extends StringValueResolver<String> {

    public static final ContentStringResolver INSTANCE = new ContentStringResolver();

    @Override
    public String resolve(String value) {
        try {
            ISObject sobj = SObjectResolver.INSTANCE.resolve(value);
            return null == sobj ? fallBack(value) : IO.readContentAsString(sobj.asInputStream());
        } catch (Exception e) {
            return fallBack(value);
        }
    }

    private String fallBack(String value) {
        Boolean mercy = attribute(ATTR_MERCY);
        if (null == mercy) {
            mercy = false;
        }
        return mercy ? value : null;
    }

}
