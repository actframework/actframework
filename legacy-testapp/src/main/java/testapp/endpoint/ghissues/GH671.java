package testapp.endpoint.ghissues;

import act.app.conf.AutoConfig;
import act.controller.annotation.UrlContext;
import act.util.JsonView;
import org.osgl.$;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.Const;

@UrlContext("671")
@AutoConfig
public class GH671 extends GithubIssueBase {
    public static class juhe {
        public static Const<String> key = $.constant();
        public static Const<String> uri = $.constant();
    }

    @GetAction
    @JsonView
    public void test() {
        String key = juhe.key.get();
        String uri = juhe.uri.get();
        render(key, uri);
    }
}
