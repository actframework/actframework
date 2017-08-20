package testapp;

/**
 * Allow the session/flash be communicate to client via HTTP header in addition to cookie
 */
public class SessionMapper extends act.util.SessionMapper.HeaderSessionMapper {

    public SessionMapper() {
        super("X-TEST-");
    }

}
