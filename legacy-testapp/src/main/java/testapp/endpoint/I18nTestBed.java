package testapp.endpoint;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.result.Result;
import testapp.TestApp;

import javax.inject.Inject;

import static act.controller.Controller.Util.text;

@UrlContext("/i18n")
public class I18nTestBed {

    @Inject
    private ActionContext context;

    @GetAction("foo")
    public Result foo() {
        return text(context.i18n("foo"));
    }

    @GetAction("template")
    public Result template(String foo, int bar) {
        return text(context.i18n("template", foo, bar));
    }

    @GetAction("yfoo")
    public Result bundleByClassFoo() {
        return text(context.i18n(TestApp.class, "foo"));
    }

}
