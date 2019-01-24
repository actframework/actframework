package test;

import act.Act;
import act.data.annotation.Data;
import act.util.LogSupport;
import act.util.SimpleBean;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.mvc.annotation.PostAction;

/**
 * A simple hello world app entry
 *
 * Run this app, try to update some of the code, then
 * press F5 in the browser to watch the immediate change
 * in the browser!
 */
@SuppressWarnings("unused")
public class AppEntry extends LogSupport {

    @Data
    public static class Foo implements SimpleBean {
        public int barLevel;
    }

    @GetAction
    public String queryParamsTestBed(String fooBar) {
        return fooBar;
    }

    @PostAction
    public Foo postBindingTestBed(Foo foo) {
        return foo;
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }

}
