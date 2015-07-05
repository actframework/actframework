package act.event;

import act.util.DestroyableBase;

public abstract class ActEventListenerBase<EVENT_TYPE extends ActEvent> extends DestroyableBase implements ActEventListener<EVENT_TYPE> {

}
