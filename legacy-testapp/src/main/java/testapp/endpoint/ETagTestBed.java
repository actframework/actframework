package testapp.endpoint;

import act.controller.Controller;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;
import testapp.model.VersionedModel;

@UrlContext("/etag")
public class ETagTestBed extends Controller.Util {

    @GetAction("{id}")
    public VersionedModel get(String id) {
        return VersionedModel.getById(id);
    }

}
