package act.controller.captcha.generator;

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

import act.act_messages;
import act.controller.captcha.CaptchaSession;
import act.controller.captcha.CaptchaSessionGenerator;
import act.i18n.I18n;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generate {@link CaptchaSession} with a string consists of 6 random alphabetic characters.
 */
public class RandomTextGenerator implements CaptchaSessionGenerator {

    @Override
    public CaptchaSession generate() {
        return new CaptchaSession(
                randomString(), null, null,
                I18n.i18n(act_messages.class, "act.captcha.generator.random_text.instruction"),
                null);
    }

    private static char[] SPACE_ = {
            '0', '1', '2', '3', '4',
            '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    public static final String SPACE = new String(SPACE_);

    private static final int SPACE_SIZE = SPACE_.length;

    private String randomString() {
        int len = 6;
        char[] chars = SPACE_;
        int charsLen = SPACE_SIZE;
        Random r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(len);
        while (len-- > 0) {
            int i = r.nextInt(charsLen);
            sb.append(chars[i]);
        }
        return sb.toString();
    }
}
