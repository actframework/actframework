package act.data;

import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.storage.ISObject;
import org.osgl.util.C;
import org.osgl.util.IO;
import org.osgl.util.StringValueResolver;

import java.util.List;

/**
 * Read content lines from resource URL
 */
public class ContentLinesBinder extends Binder<List<String>> {

    public static final ContentLinesBinder INSTANCE = new ContentLinesBinder();

    @Override
    public List<String> resolve(List<String> bean, String model, ParamValueProvider params) {
        Boolean reportingError = attribute("reportError");
        if (null == reportingError) {
            reportingError = false;
        }
        if (reportingError) {
            return IO.readLines(SObjectBinder.INSTANCE.resolve(null, model, params).asInputStream());
        } else {
            try {
                ISObject sobj = SObjectBinder.INSTANCE.resolve(null, model, params);
                return null == sobj ? fallBackList(model, params) : IO.readLines(sobj.asInputStream());
            } catch (Exception e) {
                return fallBackList(model, params);
            }
        }
    }

    private List<String> fallBackList(String model, ParamValueProvider params) {
        String val = params.paramVal(model);
        return null == val ? C.<String>list() : C.list(val);
    }

}
