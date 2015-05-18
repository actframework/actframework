package org.osgl.oms.app;

import org.osgl.oms.OmsException;

public class RequestRefreshClassLoader extends OmsException {

    public static final RequestRefreshClassLoader INSTANCE = new RequestRefreshClassLoader();

    public RequestRefreshClassLoader() {
        super();
    }

    @Override
    public String getMessage() {
        return "app class loader refresh required";
    }
}
