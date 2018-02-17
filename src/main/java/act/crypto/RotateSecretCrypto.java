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
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RotateSecretCrypto extends AppCrypto {

    private RotationSecretProvider secretProvider;
    private boolean rotationEnabled;

    @Inject
    public RotateSecretCrypto(RotationSecretProvider secretProvider) {
        super(secretProvider.rawSecret());
        this.rotationEnabled = secretProvider.isRotateEnabled();
        this.secretProvider = secretProvider;
    }

    @Override
    public String sign(String message) {
        return !rotationEnabled ? super.sign(message) : cur().sign(message);
    }

    public boolean verifySignature(String message, String signature) {
        if (!rotationEnabled) {
            return S.eq(signature, super.sign(message));
        }
        return S.eq(signature, cur().sign(message))
                || S.eq(signature, prev().sign(message))
                || S.eq(signature, next().sign(message));
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
        return !rotationEnabled ? super.encrypt(message) : cur().encrypt(message);
    }

    @Override
    public String decrypt(String message) {
        if (!rotationEnabled) {
            return super.decrypt(message);
        }
        try {
            return cur().decrypt(message);
        } catch (Exception e) {
            try {
                return prev().decrypt(message);
            } catch (Exception e1) {
                return next().decrypt(message);
            }
        }
    }

    private AppCrypto cur() {
        return new AppCrypto(secretProvider.curSecret());
    }

    private AppCrypto prev() {
        return new AppCrypto(secretProvider.lastSecret());
    }

    private AppCrypto next() {
        return new AppCrypto(secretProvider.nextSecret());
    }
}
