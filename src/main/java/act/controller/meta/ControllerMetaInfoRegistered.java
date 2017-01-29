package act.controller.meta;

import act.event.ActEvent;
import act.event.SystemEvent;

/**
 * An event triggered when a {@link ControllerClassMetaInfo} has been
 * registered
 */
public class ControllerMetaInfoRegistered extends ActEvent<ControllerClassMetaInfo> implements SystemEvent {
    public ControllerMetaInfoRegistered(ControllerClassMetaInfo source) {
        super(source);
    }
}
