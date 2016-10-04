package act.cli;

/**
 * A simple interface for authorization
 */
public interface CliOverHttpAuthority {
    /**
     * Implementation shall provide the authorization logic in this method to check
     * if the current principal has access to CliOverHttp facilities.
     *
     * If the current principal has no permission, the implementation shall provide
     * relevant logic, e.g. to throw out {@link org.osgl.mvc.result.Forbidden} or
     * throw out a {@link org.osgl.mvc.result.Redirect redirection} to login page etc
     */
    void authorize();

    class AllowAll implements CliOverHttpAuthority {
        @Override
        public void authorize() {
            // just allow it
        }
    }
}
