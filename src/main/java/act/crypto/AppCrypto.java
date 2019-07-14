package act.crypto;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.conf.AppConfig;
import act.util.Stateless;
import org.mindrot.jbcrypt.BCrypt;
import org.osgl.exception.UnexpectedException;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Provide the crypto relevant methods for application to use:
 *
 * * {@link #sign(String)}
 * * {@link #passwordHash(char[])}
 * * {@link #passwordHash(String)}
 * * {@link #verifyPassword(char[], char[])}
 * * {@link #verifyPassword(char[], String)}
 * * {@link #verifyPassword(String, String)}
 * * {@link #encrypt(String)}
 * * {@link #decrypt(String)}
 * * {@link #generateToken(String, String...)}
 * * {@link #generateToken(int, String, String...)}
 * * {@link #generateToken(Token.Life, String, String...)}
 *
 * Note all methods listed above are executed with {@link act.conf.AppConfigKey#SECRET confiured application secret}
 */
@Stateless
public class AppCrypto {

    protected static final Logger LOGGER = LogManager.get(AppCrypto.class);

    private byte[] secret;

    private SecureRandom secureRandom = new SecureRandom();

    protected AppCrypto() {}

    /**
     * Construct an `AppCrypto` instance with given secret.
     *
     * @param secret the secret to be used with all crypto relevant methods
     */
    public AppCrypto(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Construct an `AppCrypto` instance with {@link AppConfig application config}.
     *
     * Note this constructor is not supposed to be called by application.
     *
     * @param config the application config.
     */
    public AppCrypto(AppConfig config) {
        secret = config.secret().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns signature of given message string.
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param message the string to be signed.
     * @return the signature of the string
     */
    public String sign(String message) {
        return Crypto.sign(message, secret);
    }

    /**
     * Generate crypted hash of given password. This method is more secure than
     * {@link #passwordHash(String)} as it will fill the password char array
     * with `\0` once used.
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * See <a href="http://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords-in-java">This SO for more detail</a>
     * @param password the password
     * @return the password hash
     */
    public char[] passwordHash(char[] password) {
        if (null == password) {
            return null;
        }
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Generate crypted hash of give password.
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param password the password
     * @return the password hash
     */
    public String passwordHash(String password) {
        if (null == password) {
            return null;
        }
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Verify a password against given hash.
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param password the password to be verified.
     * @param hash the hash used to verify the password
     * @return `true` if the password can be verified with the given hash, or `false` otherwise.
     */
    public boolean verifyPassword(String password, String hash) {
        if (null == password) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verify a password against given hash.
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param password the password to be verified.
     * @param hash the hash used to verify the password
     * @return `true` if the password can be verified with the given hash, or `false` otherwise.
     */
    public boolean verifyPassword(char[] password, String hash) {
        if (null == password) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verify a password against given hash.
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param password the password to be verified.
     * @param hash the hash used to verify the password
     * @return `true` if the password can be verified with the given hash, or `false` otherwise.
     */
    public boolean verifyPassword(char[] password, char[] hash) {
        if (null == password) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, new String(hash));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns encrypted message.
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param message the message to be encrypted
     * @return the encrypted message
     */
    public String encrypt(String message) {
        try {
            return Crypto.encryptAES(message, secret);
        } catch (UnexpectedException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InvalidKeyException) {
                LOGGER.error("Cannot encrypt/decrypt! Please download Java Crypto Extension pack from Oracle: http://www.oracle.com/technetwork/java/javase/tech/index-jsp-136007.html");
            }
            throw e;
        }
    }

    /**
     * Returns decrypted message.
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param message the message to be decrypted.
     * @return the decrypted message.
     */
    public String decrypt(String message) {
        try {
            return Crypto.decryptAES(message, secret);
        } catch (UnexpectedException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InvalidKeyException) {
                LOGGER.error("Cannot encrypt/decrypt! Please download Java Crypto Extension pack from Oracle: http://www.oracle.com/technetwork/java/javase/tech/index-jsp-136007.html");
            }
            throw e;
        }
    }

    /**
     * Returns checksum of the content of a given input stream.
     * @param is the input stream
     * @return the checksum as described above.
     */
    public String checksum(InputStream is) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] dataBytes = new byte[1024];

            int nread;

            while ((nread = is.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }

            byte[] mdbytes = md.digest();
            S.Buffer sb = S.buffer();
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();

        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    /**
     * Generate random int from a {@link SecureRandom} instance.
     * @param max the bound of the random number to be returned. must be positive.
     * @return a random number between `0` and `max`
     */
    public int generateRandomInt(int max) {
        return secureRandom.nextInt(max);
    }

    /**
     * Generate a random number from a {@link SecureRandom} using {@link Integer#MAX_VALUE} as the bound.
     * @return a random number between `0` and {@link Integer#MAX_VALUE}
     */
    public int generateRandomInt() {
        return secureRandom.nextInt(Integer.MAX_VALUE);
    }

    /**
     * Generate an ecrypted token string. The life of the string generated is {@link Token.Life#SHORT}, i.e. 1 hour.
     *
     * One typical scenario that can use this method is when a new user registered with an email address, the app
     * would like to send an email with a link to verify the user. Here is how to do it:
     *
     * Define a PostOffice that can send out email
     *
     * ```java
     * {@literal @}Mailer
     * {@literal @}TemplateContext("mail")
     * public class PostOffice extends Mailer.Util {
     *     {@literal @}inject
     *     private AppCrypto crypto;
     *     public void sendWelcomeLetter(User user) {
     *         to(user.email);
     *         subject("Welcome to our super system");
     *         from("noreply@mycom.com");
     *         String token = crypto.generateToken(email);
     *         send(user, token);
     *     }
     * }
     * ```
     * <p></p>
     * Create a template file `resources/rythm/mail/sendWelcomeLetter.html`:
     *
     * ```html
     * <html>
     * <head></head>
     * <body>
     * {@literal @}args User user, String token
     *
     * Please click the following link to verify your registration:
     *
     * <p>
     *   <a href='http://@(_conf.host())/confirmSignUp?token=@token'>http://@(_conf.host())/confirmSignUp?token=@token</a>
     * </p>
     * <p>
     *   Note, this link can only be used once.
     * </p>
     * </body>
     * </html>
     * ```
     * <p></p>
     * Then define a request handler to process new user registration
     *
     * ```java
     * {@literal @}PostAction("signUp")
     * public void handleSignUp(String email, String password, User.Dao userDao, PostOffice postOffice) {
     *     User user = new User(email, password);
     *     userDao.save(user);
     *     postOffice.sendWelcomeLetter(user);
     * }
     * ```
     * <p></p>
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param id the id of the token
     * @param payload other payloads of the token.
     * @return an encrypted token string enbodies the `id` and `payloads`.
     */
    public String generateToken(String id, String... payload) {
        return Token.generateToken(secret, id, payload);
    }

    /**
     * Generate an ecrypted token string with life (expiry time) specified.
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param expiration specify how long the generated string can live before expire.
     * @param id the id of the token
     * @param payload other payloads of the token.
     * @return an encrypted token string enbodies the `id` and `payloads`.
     */
    public String generateToken(Token.Life expiration, String id, String ... payload) {
        return Token.generateToken(secret, expiration, id, payload);
    }

    /**
     * Generate an ecrypted token string with life (expiry time) specified in seconds.
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param seconds specify how many seconds the generated string can live before expire.
     * @param id the id of the token
     * @param payload other payloads of the token.
     * @return an encrypted token string enbodies the `id` and `payloads`.
     */
    public String generateToken(int seconds, String id, String ... payload) {
        return Token.generateToken(secret, seconds, id, payload);
    }

    /**
     * Parse a generated token string and returns a {@link Token} instance.
     *
     * A typical usage scenario of this method is to verify new user registration confirmation link sent
     * to a welcome email. Refer to {@link #generateToken(String, String...)}
     *
     * ```java
     * {@literal @}GetAction("confirmSignUp")
     * public void confirmSignUp(String token, AppCrypto crypto, User.Dao userDao) {
     *     Token tk = crypto.parseToken(token);
     *     notFoundIfNot(tk.isValid());
     *     String email = tk.id();
     *     User user = userDao.findOneBy("email", email);
     *     notFoundIfNull(user);
     *     user.markAsConfirmed();
     *     userDao.save(user);
     *     redirect("/welcome");
     * }
     * ```
     *
     * Note this method uses {@link act.conf.AppConfigKey#SECRET confiured application secret}
     *
     * @param tokenString the token string (generated wth `generateToken` call before.
     * @return a `Token` instance.
     */
    public Token parseToken(String tokenString) {
        return Token.parseToken(secret, tokenString);
    }

}
