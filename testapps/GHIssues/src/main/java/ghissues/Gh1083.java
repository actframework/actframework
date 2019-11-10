package ghissues;

import act.controller.annotation.TemplateContext;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;
import org.osgl.storage.ISObject;

@UrlContext("1083")
@TemplateContext("1083")
public class Gh1083 extends BaseController {
    @GetAction
    public void form() {
    }

    @PostAction
    public boolean handleUpload(ISObject file, String fileName) {
        return null == file;
    }
}
