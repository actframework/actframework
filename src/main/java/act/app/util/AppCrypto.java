package act.app.util;

import act.app.App;
import act.conf.AppConfig;
import org.apache.commons.codec.Charsets;
import org.mindrot.jbcrypt.BCrypt;
import org.osgl.exception.UnexpectedException;
import org.osgl.logging.Logger;
import org.osgl.util.Crypto;
import org.osgl.util.Token;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;

public class AppCrypto {

    private static Logger logger = App.logger;
    
    private byte[] secret;

    private SecureRandom secureRandom = new SecureRandom();
    
    public AppCrypto(AppConfig config) {
        secret = config.secret().getBytes(Charsets.UTF_8);
    }

    public String sign(String message) {
        return Crypto.sign(message, secret);
    }

    /**
     * Generate crypted hash of given password. This method is more secure than
     * {@link #passwordHash(String)} as it will fill the password char array
     * with `\0` once used.
     *
     * See <a href="http://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords-in-java">This SO for more detail</a>
     * @param password the password
     * @return the password hash
     */
    public String passwordHash(char[] password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Generate crypted hash of give password
     * @param password the password
     * @return the password hash
     */
    public String passwordHash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean verifyPassword(String password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean verifyPassword(char[] password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            return false;
        }
    }

    public String encrypt(String message) {
        try {
            return Crypto.encryptAES(message, secret);
        } catch (UnexpectedException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InvalidKeyException) {
                logger.error("Cannot encrypt/decrypt! Please download Java Crypto Extension pack from Oracle: http://www.oracle.com/technetwork/java/javase/tech/index-jsp-136007.html");
            }
            throw e;
        }
    }

    public String decrypt(String message) {
        try {
            return Crypto.decryptAES(message, secret);
        } catch (UnexpectedException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InvalidKeyException) {
                logger.error("Cannot encrypt/decrypt! Please download Java Crypto Extension pack from Oracle: http://www.oracle.com/technetwork/java/javase/tech/index-jsp-136007.html");
            }
            throw e;
        }
    }

    public int generateRandomInt(int max) {
        return secureRandom.nextInt(max);
    }

    public int generateRandomInt() {
        return secureRandom.nextInt(Integer.MAX_VALUE);
    }

    public String generateToken(String id, String... payload) {
        return Token.generateToken(secret, id, payload);
    }

    public String generateToken(Token.Life expiration, String id, String ... payload) {
        return Token.generateToken(secret, expiration, id, payload);
    }

    public String generateToken(int seconds, String id, String ... payload) {
        return Token.generateToken(secret, seconds, id, payload);
    }

    public Token parseToken(String tokenString) {
        return Token.parseToken(secret, tokenString);
    }

}
