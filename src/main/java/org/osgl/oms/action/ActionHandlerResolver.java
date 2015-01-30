package org.osgl.oms.action;

public interface ActionHandlerResolver {
    ActionHandler resolve(CharSequence payload);
}
