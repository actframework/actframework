package act.security;

import act.conf.AppConfig;
import org.osgl.http.H;

public class UsernameSecureTicketCodecTest extends StringSecureTicketCodecTest {

    private static final String sessionKeyUsername = new AppConfig<>().sessionKeyUsername();

    protected StringSecureTicketCodec codec() {
        return new UsernameSecureTicketCodec(crypto(), sessionKeyUsername);
    }

    @Override
    protected void prepareSession(H.Session session) {
        session.put(sessionKeyUsername, "abc@123.com");
    }

    @Override
    public void verifySession(H.Session original, H.Session decoded) {
        eq(original.get(sessionKeyUsername), decoded.get(sessionKeyUsername));
    }
}
