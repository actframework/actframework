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

import javax.crypto.Mac;

public class RotateSecretHMAC extends HMAC {

    private RotationSecretProvider secretProvider;
    private boolean rotateEnabled;

    public RotateSecretHMAC(String algoKey, RotationSecretProvider secretProvider) {
        super(secretProvider.rawSecret(), algoKey);
        this.secretProvider = $.requireNotNull(secretProvider);
        this.rotateEnabled = secretProvider.isRotateEnabled();
    }

    public RotateSecretHMAC(Algorithm algo, RotationSecretProvider secretProvider) {
        super(secretProvider.rawSecret(), algo);
        this.secretProvider = secretProvider;
    }

    @Override
    protected boolean verifyHash(byte[] payload, byte[] hash) {
        if (!rotateEnabled) {
            return super.verifyHash(payload, hash);
        }
        return verifyHash(payload, hash, curMac())
                || verifyHash(payload, hash, prevMac())
                || verifyHash(payload, hash, nextMac());
    }

    @Override
    protected byte[] doHash(byte[] bytes) {
        return rotateEnabled ? doHash(bytes, curMac()) : super.doHash(bytes);
    }

    private Mac curMac() {
        return algo.macOf(secretProvider.curSecret());
    }

    private Mac prevMac() {
        return algo.macOf(secretProvider.lastSecret());
    }

    private Mac nextMac() {
        return algo.macOf(secretProvider.nextSecret());
    }
}
