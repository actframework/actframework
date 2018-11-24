package test;

import act.Act;
import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import org.osgl.mvc.annotation.GetAction;

@UrlContext("968")
public class AppEntry {

    @GetAction("meet")
    public String meet(ActionContext ctx) {
        return ctx.session().get("Hi");
    }

    @GetAction("touch")
    public String touch(ActionContext ctx) {
        ctx.session().put("Hi", "Hi");
        return "Hello";
    }

    public static void main(String[] args) throws Exception {
        Act.start();
    }
}
