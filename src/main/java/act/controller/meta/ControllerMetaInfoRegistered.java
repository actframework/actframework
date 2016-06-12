package act.controller.meta;

import act.event.ActEvent;

/**
 * An event triggered when a {@link ControllerClassMetaInfo} has been
 * registered
 */
public class ControllerMetaInfoRegistered extends ActEvent<ControllerClassMetaInfo> {
    public ControllerMetaInfoRegistered(ControllerClassMetaInfo source) {
        super(source);
    }
}
