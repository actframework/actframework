package act.security;

import act.Act;
import act.app.util.AppCrypto;
import act.conf.AppConfig;

/**
 * A simple implementation of {@link SecureTicketCodec} that process
 * `id` and `username` in session only
 */
public class UsernameSecureTicketCodec extends StringSecureTicketCodec {

    public UsernameSecureTicketCodec() {
        super(Act.appConfig().sessionKeyUsername());
    }

    public UsernameSecureTicketCodec(AppConfig config) {
        super(config.sessionKeyUsername());
    }

    public UsernameSecureTicketCodec(String sessinKeyUsername) {
        super(sessinKeyUsername);
    }


    public UsernameSecureTicketCodec(AppCrypto crypto) {
        super(crypto, Act.appConfig().sessionKeyUsername());
    }

    public UsernameSecureTicketCodec(AppCrypto crypto, AppConfig config) {
        super(crypto, config.sessionKeyUsername());
    }

    public UsernameSecureTicketCodec(AppCrypto crypto, String sessinKeyUsername) {
        super(crypto, sessinKeyUsername);
    }


}
