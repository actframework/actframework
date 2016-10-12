package act.data;

import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.IO;

import java.util.List;

import static act.data.annotation.ReadContent.ATTR_MERCY;

/**
 * Read content lines from resource URL
 */
public class ContentLinesBinder extends Binder<List<String>> {

    public static final ContentLinesBinder INSTANCE = new ContentLinesBinder();

    @Override
    public List<String> resolve(List<String> bean, String model, ParamValueProvider params) {
        try {
            ISObject sobj = SObjectBinder.INSTANCE.resolve(null, model, params);
            return null == sobj ? fallBack(model, params) : IO.readLines(sobj.asInputStream());
        } catch (Exception e) {
            return fallBack(model, params);
        }
    }

    private List<String> fallBack(String model, ParamValueProvider params) {
        Boolean mercy = attribute(ATTR_MERCY);
        if (null == mercy) {
            mercy = false;
        }
        if (mercy) {
            String val = params.paramVal(model);
            return null == val ? C.<String>list() : C.list(val);
        }
        return C.list();
    }

}
