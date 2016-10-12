package act.data;

import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.storage.ISObject;
import org.osgl.util.IO;

import static act.data.annotation.ReadContent.ATTR_MERCY;

/**
 * Read content as string from resource URL
 */
public class ContentStringBinder extends Binder<String> {

    public static final ContentStringBinder INSTANCE = new ContentStringBinder();

    @Override
    public String resolve(String bean, String model, ParamValueProvider params) {
        try {
            ISObject sobj = SObjectBinder.INSTANCE.resolve(null, model, params);
            return null == sobj ? fallBack(model, params) : IO.readContentAsString(sobj.asInputStream());
        } catch (Exception e) {
            return fallBack(model, params);
        }
    }

    private String fallBack(String model, ParamValueProvider params) {
        Boolean mercy = attribute(ATTR_MERCY);
        if (null == mercy) {
            mercy = false;
        }
        return mercy ? params.paramVal(model) : null;
    }


}
