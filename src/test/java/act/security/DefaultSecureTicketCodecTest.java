package act.security;

public class DefaultSecureTicketCodecTest extends SecureTicketCodecTest {

    protected SecureTicketCodec codec() {
        return new DefaultSecureTicketCodec(crypto());
    }


}
