package ghissues;

import act.app.event.SysEventId;
import act.event.EventBus;
import act.job.OnSysEvent;

public class Gh1206 {

    /**
     * See https://github.com/actframework/actframework/issues/1206
     *
     * Gocha:DO NOT declare event listener using OnSysEvent
     * annotation before SINGLETON_PROVISIONED which is raised
     * after DEPENDENCY_INJECTOR_PROVISIONED event
     */
    @OnSysEvent(SysEventId.SINGLETON_PROVISIONED)
    public void on(EventBus eventBus) {

        assert eventBus != null;
    }
}
