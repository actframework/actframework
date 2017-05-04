package act.security;

public class DefaultSecureTicketCodecTest extends StringSecureTicketCodecTest {

    protected StringSecureTicketCodec codec() {
        return new DefaultSecureTicketCodec(crypto());
    }

}
