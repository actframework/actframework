package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.inject.annotation.Configuration;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.Const;

@UrlContext("536")
public class GH536 extends GithubIssueBase {

    @Configuration("gh536.conf")
    public static String conf;

    @Configuration("gh536.conf")
    public static final Const<String> CONST_FINAL_CONF = $.constant();

    @Configuration("gh536.conf")
    public static Const<String> CONST_CONF;

    @Configuration("gh536.conf")
    public static final Osgl.Val<String> VAL_FINAL_CONF = $.val("");

    @Configuration("gh536.conf")
    public static $.Val<String> VAL_CONF;


    @GetAction
    public Object test() {
        return conf;
    }

    @GetAction("const_final")
    public Object testConstFinal() {
        return CONST_FINAL_CONF.get();
    }

    @GetAction("const")
    public Object testConst() {
        return CONST_CONF.get();
    }

    @GetAction("val_final")
    public Object testValFinal() {
        return VAL_FINAL_CONF.get();
    }

    @GetAction("val")
    public Object testVal() {
        return VAL_CONF.get();
    }

}
