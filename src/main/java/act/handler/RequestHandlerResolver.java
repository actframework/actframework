package act.handler;

public interface RequestHandlerResolver {
    RequestHandler resolve(CharSequence payload);
}
