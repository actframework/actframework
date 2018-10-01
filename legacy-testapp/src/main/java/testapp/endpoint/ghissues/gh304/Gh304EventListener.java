package testapp.endpoint.ghissues.gh304;

import act.app.event.SysEventId;
import act.event.ActEventListenerBase;
import act.event.BindOn;
import act.event.EventBus;
import act.job.OnSysEvent;
import org.osgl.mvc.annotation.GetAction;

import javax.inject.Singleton;

@Singleton
@BindOn(SysEventId.SINGLETON_PROVISIONED)
public class Gh304EventListener extends ActEventListenerBase<Gh304Event> {

    private boolean triggered;

    @Override
    public void on(Gh304Event event) throws Exception {
        triggered = true;
    }

    @GetAction("/gh/304")
    public boolean checkState() {
        return triggered;
    }

    @OnSysEvent(SysEventId.PRE_START)
    public static void triggerEvent(EventBus eventBus) {
        eventBus.trigger(new Gh304Event());
    }
}
