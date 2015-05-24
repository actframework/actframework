package act.handler;

import org.osgl._;
import org.osgl.exception.NotAppliedException;

public abstract class RequestHandlerResolverBase extends _.F1<String, RequestHandler> implements RequestHandlerResolver {

    @Override
    public RequestHandler apply(String s) throws NotAppliedException, _.Break {
        return resolve(s);
    }

}
