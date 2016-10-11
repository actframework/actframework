package act.data;

import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.util.IO;

/**
 * Read content as string from resource URL
 */
public class ContentStringBinder extends Binder<String> {

    public static final ContentStringBinder INSTANCE = new ContentStringBinder();

    @Override
    public String resolve(String bean, String model, ParamValueProvider paramValueProvider) {
        Boolean reportingError = attribute("reportError");
        if (null == reportingError) {
            reportingError = false;
        }
        if (reportingError) {
            return IO.readContentAsString(SObjectBinder.INSTANCE.resolve(null, model, paramValueProvider).asInputStream());
        } else {
            try {
                return IO.readContentAsString(SObjectBinder.INSTANCE.resolve(null, model, paramValueProvider).asInputStream());
            } catch (Exception e) {
                return paramValueProvider.paramVal(model);
            }
        }
    }

}
