package org.osgl.oms.handler;

public interface RequestHandlerResolver {
    RequestHandler resolve(CharSequence payload);
}
