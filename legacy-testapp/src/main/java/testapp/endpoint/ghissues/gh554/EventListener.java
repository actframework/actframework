package testapp.endpoint.ghissues.gh554;

import act.app.App;
import act.event.EventBus;
import act.event.On;
import org.osgl.mvc.result.RenderText;

public class EventListener {
    @On("message-send")
    public void send(MsgTemplate template) {
        throw new RenderText(template.id);
    }

    public static void excute(MsgTemplate template) {
        EventBus eventBus = App.instance().getInstance(EventBus.class);
        eventBus.trigger("message-send", template);
    }
}
