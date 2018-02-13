package act.crypto;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import act.session.RotationSecretProvider;
import org.osgl.$;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RotateSecretCrypto extends AppCrypto {

    private RotationSecretProvider secretProvider;

    @Inject
    public RotateSecretCrypto(RotationSecretProvider secretProvider) {
        this.secretProvider = $.notNull(secretProvider);
    }

    @Override
    public String sign(String message) {
        return super.sign(message);
    }

    @Override
    public char[] passwordHash(char[] password) {
        return super.passwordHash(password);
    }

    @Override
    public String passwordHash(String password) {
        return super.passwordHash(password);
    }

    @Override
    public boolean verifyPassword(String password, String hash) {
        return super.verifyPassword(password, hash);
    }

    @Override
    public boolean verifyPassword(char[] password, String hash) {
        return super.verifyPassword(password, hash);
    }

    @Override
    public boolean verifyPassword(char[] password, char[] hash) {
        return super.verifyPassword(password, hash);
    }

    @Override
    public String encrypt(String message) {
        return super.encrypt(message);
    }

    @Override
    public String decrypt(String message) {
        return super.decrypt(message);
    }
}
