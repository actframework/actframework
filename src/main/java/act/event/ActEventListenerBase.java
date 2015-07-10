package act.event;

import act.util.DestroyableBase;

import java.util.EventObject;

public abstract class ActEventListenerBase<EVENT_TYPE extends EventObject> extends DestroyableBase implements ActEventListener<EVENT_TYPE> {

}
