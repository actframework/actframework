package testapp.endpoint.ghissues.gh471;

import act.app.ActionContext;
import act.controller.annotation.UrlContext;
import act.event.EventBus;
import act.event.OnEvent;
import org.osgl.mvc.annotation.GetAction;
import testapp.endpoint.ghissues.GithubIssueBase;

@UrlContext("471")
public class GH471 extends GithubIssueBase {

    @OnEvent
    public void handleFoo(FooEvent event, ActionContext context) {
        context.attribute("foo", event.getSource());
    }

    @GetAction("foo/{foo}")
    public String testFoo(String foo, EventBus eventBus) {
        FooEvent event = new FooEvent(foo);
        eventBus.trigger(event);
        return context.attribute("foo");
    }

    @OnEvent
    public void handleFoo2(FooEvent event, String suffix, ActionContext context) {
        context.attribute("foo", event.getSource() + suffix);
    }

    @GetAction("foo/{foo}/{suffix}")
    public String testFoo2(String foo, String suffix, EventBus eventBus) {
        FooEvent event = new FooEvent(foo);
        eventBus.trigger(event, suffix);
        return context.attribute("foo");
    }

}
