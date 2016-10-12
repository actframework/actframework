package act.data;

import act.app.ActionContext;
import org.osgl.$;
import org.osgl.mvc.util.Binder;
import org.osgl.mvc.util.ParamValueProvider;

import java.io.File;

/**
 * Resolve file uploads
 */
public class FileBinder extends Binder<File> {

    @Override
    public File resolve(File file, String s, ParamValueProvider paramValueProvider) {
        ActionContext ctx = $.cast(paramValueProvider);
        return ctx.upload(s).asFile();
    }
}
