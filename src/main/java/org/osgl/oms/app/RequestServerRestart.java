package org.osgl.oms.app;

import org.osgl.oms.OmsException;

public class RequestServerRestart extends OmsException {

    public RequestServerRestart() {
        super();
    }

    @Override
    public String getMessage() {
        return "OMS restart needed";
    }
}
