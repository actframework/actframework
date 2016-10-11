package act.data;

import act.app.ActionContext;
import org.osgl.$;
import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.storage.ISObject;
import org.osgl.util.E;

/**
 * Resolve file uploads
 */
public class SObjectBinder extends Binder<ISObject> {

    public static final SObjectBinder INSTANCE = new SObjectBinder();

    @Override
    public ISObject resolve(ISObject sobj, String s, ParamValueProvider paramValueProvider) {
        E.illegalArgumentIf(!(paramValueProvider instanceof ActionContext));
        ActionContext ctx = $.cast(paramValueProvider);
        return ctx.upload(s);
    }
}
