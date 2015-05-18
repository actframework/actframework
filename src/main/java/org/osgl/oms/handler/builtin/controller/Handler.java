package org.osgl.oms.handler.builtin.controller;

import org.osgl._;
import org.osgl.mvc.result.RenderBinary;
import org.osgl.mvc.result.RenderJSON;
import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

/**
 * The base class of @Before, @After, @Exception, @Finally interceptor and
 * request dispatcher
 */
public abstract class Handler<T extends Handler> implements Comparable<T> {

    private int priority;

    protected Handler(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(T o) {
        return priority - o.priority();
    }

    @Override
    public int hashCode() {
        return _.hc(priority, getClass());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return obj.getClass().equals(getClass());
    }

}
