package act.handler.builtin;

/**
 * This is a tag interface that when a {@link act.handler.RequestHandler} implement this
 * means the underline network can invoke the handler directly in IO thread instead of worker
 * thread
 */
public interface DirectIO {
}
