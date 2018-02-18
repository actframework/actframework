package testapp.endpoint.ghissues;

import act.controller.annotation.UrlContext;
import act.event.EventBus;
import act.event.On;
import org.osgl.mvc.annotation.GetAction;
import org.osgl.storage.impl.SObject;

@UrlContext("518")
public class GH518 extends GithubIssueBase{

    @On("ECHO")
    public void eventHandler(SObject sobj) {
        throw renderText(sobj.asString());
    }

    @GetAction("{msg}")
    public void test(String msg, EventBus eventBus) {
        eventBus.emit("ECHO", SObject.of(msg));
    }

}
