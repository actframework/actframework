package act.route;

import act.app.ActAppException;
import act.app.SourceInfo;
import org.osgl.$;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class DuplicateRouteMappingException extends ActAppException {

    private RouteInfo existingRouteMapping;
    private RouteInfo newRouteMapping;

    public DuplicateRouteMappingException(RouteInfo existingRoute, RouteInfo newRoute) {
        existingRouteMapping = $.notNull(existingRoute);
        newRouteMapping = $.notNull(newRoute);
    }

    @Override
    public String getErrorTitle() {
        return "Duplicate route mapping";
    }

    @Override
    public String getMessage() {
        return getErrorDescription();
    }

    @Override
    public String getErrorDescription() {
        return S.fmt("Can not overwrite existing route mapping:\n\t%s\nwith new route mapping:\n\t%s", existingRouteMapping, newRouteMapping);
    }

}
