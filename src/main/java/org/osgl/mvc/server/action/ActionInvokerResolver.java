package org.osgl.mvc.server.action;

public interface ActionInvokerResolver {
    ActionInvoker resolve(CharSequence name);
}
