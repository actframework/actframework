package org.osgl.oms.controller;

import org.osgl._;
import org.osgl.mvc.result.NoResult;
import org.osgl.mvc.result.RenderBinary;
import org.osgl.mvc.result.RenderJSON;
import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.plugin.Plugin;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

/**
 * The base class of @Before, @After, @Exception, @Finally interceptor and
 * request dispatcher
 */
public abstract class Interceptor<T extends Interceptor> implements Comparable<T> {

    private int priority;

    protected Interceptor(int priority) {
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


    protected static Result inferResult(Result r, AppContext appContext) {
        return r;
    }

    protected static Result inferResult(String s, AppContext appContext) {
        if (appContext.isJSON()) {
            return new RenderJSON(s);
        }
        throw E.tbd("render template use the string");
    }

    protected static Result inferResult(Map<String, ?> map, AppContext appContext) {
        if (appContext.isJSON()) {
            return new RenderJSON(map);
        }
        throw E.tbd("render template with render args in map");
    }

    protected static Result inferResult(Object[] array, AppContext appContext) {
        if (appContext.isJSON()) {
            return new RenderJSON(array);
        }
        throw E.tbd("render template with render args in array");
    }

    protected static Result inferResult(InputStream is, AppContext appContext) {
        if (appContext.isJSON()) {
            return new RenderJSON(IO.readContentAsString(is));
        } else {
            return new RenderBinary(is, null, true);
        }
    }

    protected static Result inferResult(File file, AppContext appContext) {
        if (appContext.isJSON()) {
            return new RenderJSON(IO.readContentAsString(file));
        } else {
            return new RenderBinary(file);
        }
    }

    protected static Result inferResult(Object v, AppContext appContext) {
        if (null == v) {
            return new NoResult();
        } else if (v instanceof Result) {
            return inferResult((Result) v, appContext);
        } else if (v instanceof String) {
            return inferResult((String) v, appContext);
        } else if (v instanceof InputStream) {
            return inferResult((InputStream) v, appContext);
        } else if (v instanceof File) {
            return inferResult((File) v, appContext);
        } else if (v instanceof Map) {
            return inferResult((Map) v, appContext);
        } else if (v instanceof Object[]) {
            return inferResult((Object[]) v, appContext);
        } else {
            if (appContext.isJSON()) {
                return new RenderJSON(v);
            } else {
                return inferResult(v.toString(), appContext);
            }
        }
    }
}
