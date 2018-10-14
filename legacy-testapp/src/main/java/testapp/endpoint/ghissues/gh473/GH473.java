package testapp.endpoint.ghissues.gh473;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.event.EventBus;
import act.event.On;
import act.event.OnEvent;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.util.S;
import testapp.endpoint.ghissues.GithubIssueBase;

@UrlContext("473")
public class GH473 extends GithubIssueBase {

    @OnEvent
    public void handleFoo3(Foo foo, Object[] args, ActionContext context) {
        Object arg = null == args || args.length == 0 ? "" : args[0];
        context.attribute("foo3", foo.name() + arg);
    }

    @OnEvent
    public void handleFoo(Foo foo, String suffix, ActionContext context) {
        context.attribute("foo", foo + suffix);
    }

    @OnEvent
    public void handleFoo2(Foo foo, int level, ActionContext context) {
        context.attribute("foo", foo.name() + level);
    }

    @GetAction
    public String test(String suffix, Integer level, EventBus eventBus) {
        if (null != level) {
            eventBus.trigger(Foo.FOO, level);
        } else {
            eventBus.trigger(Foo.BAR, suffix);
        }
        return S.concat(context.attribute("foo"), context.attribute("foo3"));
    }

    @On("FOO")
    public void handleFooString3(Object[] args, ActionContext context) {
        Object arg = null == args || args.length == 0 ? "" : args[0];
        context.attribute("foo3", S.string(arg));
    }

    @On("FOO")
    public void handleFooString(String suffix, ActionContext context) {
        context.attribute("foo", suffix);
    }

    @On("FOO")
    public void handleFooString2(int level, ActionContext context) {
        context.attribute("foo", S.string(level));
    }

    @GetAction("str")
    public String testString(String suffix, Integer level, EventBus eventBus) {
        if (null != level) {
            eventBus.trigger("FOO", level);
        } else {
            eventBus.trigger("FOO", suffix);
        }
        return S.concat(context.attribute("foo"), context.attribute("foo3"));
    }
}
