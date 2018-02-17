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

public class AppCrypto {

    protected static final Logger LOGGER = LogManager.get(AppCrypto.class);

    private byte[] secret;

    private SecureRandom secureRandom = new SecureRandom();

    protected AppCrypto() {}

    public AppCrypto(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }
    
    public AppCrypto(AppConfig config) {
        secret = config.secret().getBytes(StandardCharsets.UTF_8);
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
    public char[] passwordHash(char[] password) {
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

    public boolean verifyPassword(char[] password, char[] hash) {
        try {
            return BCrypt.checkpw(password, new String(hash));
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
                LOGGER.error("Cannot encrypt/decrypt! Please download Java Crypto Extension pack from Oracle: http://www.oracle.com/technetwork/java/javase/tech/index-jsp-136007.html");
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
                LOGGER.error("Cannot encrypt/decrypt! Please download Java Crypto Extension pack from Oracle: http://www.oracle.com/technetwork/java/javase/tech/index-jsp-136007.html");
            }
            throw e;
        }
    }

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
