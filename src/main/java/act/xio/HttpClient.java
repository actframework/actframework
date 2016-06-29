package act.xio;

import org.osgl.http.H;

/**
 * Send a request out to remote HTTP service and get the response
 */
public interface HttpClient {
    H.Response send(H.Request request);
}
