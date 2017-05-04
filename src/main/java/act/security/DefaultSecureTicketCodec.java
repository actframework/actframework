package act.security;

import act.app.util.AppCrypto;

import javax.inject.Singleton;

/**
 * Default implementation of {@link SecureTicketCodec}. This implementation
 * encode everything from the session object into the secure ticket
 */
@Singleton
public class DefaultSecureTicketCodec extends StringSecureTicketCodec {
    public DefaultSecureTicketCodec() {
        super();
    }

    public DefaultSecureTicketCodec(AppCrypto crypto) {
        super(crypto);
    }


}
