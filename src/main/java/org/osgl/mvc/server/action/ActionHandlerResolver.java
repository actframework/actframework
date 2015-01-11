package org.osgl.mvc.server.action;

public interface ActionHandlerResolver {
    ActionHandler resolve(CharSequence payload);
}
