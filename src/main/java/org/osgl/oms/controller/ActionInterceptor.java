package org.osgl.oms.controller;

import com.sun.corba.se.spi.orbutil.fsm.Input;
import org.osgl.http.H;
import org.osgl.mvc.result.NoResult;
import org.osgl.mvc.result.RenderBinary;
import org.osgl.mvc.result.RenderJSON;
import org.osgl.mvc.result.Result;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.conf.AppConfig;
import org.osgl.util.E;
import org.osgl.util.IO;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

/**
 * Base class of Before/After interceptor
 */
public abstract class ActionInterceptor<T extends ActionInterceptor> extends Interceptor<T> {

    protected ActionInterceptor(int priority) {
        super(priority);
    }

    public abstract Result handle(AppContext appContext);
}
