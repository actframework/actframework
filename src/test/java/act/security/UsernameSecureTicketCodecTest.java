package act.security;

import act.conf.AppConfig;
import org.osgl.http.H;

public class UsernameSecureTicketCodecTest extends SecureTicketCodecTest {

    private String sessionKeyUsername = new AppConfig().sessionKeyUsername();

    protected SecureTicketCodec codec() {
        return new UsernameSecureTicketCodec(new AppConfig(), crypto());
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
