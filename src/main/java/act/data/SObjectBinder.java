package act.data;

import act.app.ActionContext;
import org.osgl.$;
import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;
import org.osgl.storage.ISObject;

import java.io.File;

/**
 * Resolve file uploads
 */
public class SObjectBinder extends Binder<ISObject> {
    @Override
    public ISObject resolve(String s, ParamValueProvider paramValueProvider) {
        ActionContext ctx = $.cast(paramValueProvider);
        return ctx.upload(s);
    }
}
