package act.controller.captcha;

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

import act.Act;
import act.act_messages;
import act.i18n.I18n;
import org.osgl.util.S;

import java.io.Serializable;
import java.util.Set;

/**
 * A `CaptchaSession` is composed of a text (used to generate media)
 * , a corresponding answer and a media URL
 */
public class CaptchaSession implements Serializable {

    private static final long serialVersionUID = 3648122710489336079L;

    private String caption;
    private String text;
    private String answer;
    private String mediaUrl;
    private String instruction;
    private Set<String> options;


    /**
     * Construct a CAPTCHA session with text, an answer and a media URL.
     *
     * @param text the text literal
     * @param answer
     *      the answer, optional. When omitted then the answer should be
     *      the text.
     * @param mediaUrl
     *      the media URL. When omitted, then the URL is /~/captcha/{encrypted-text}.
     * @param instruction
     *      the instruction to guide how end user should treat the CAPTCHA.
     * @param options
     *      the optional a set of option from which the answer could be selected.
     */
    public CaptchaSession(String text, String answer, String mediaUrl, String instruction, Set<String> options) {
        this.caption = I18n.i18n(act_messages.class, "act.captcha.caption");
        this.text = S.requireNotBlank(text);
        this.answer = answer;
        this.mediaUrl = ensureMediaUrl(mediaUrl, text);
        this.instruction = S.requireNotBlank(instruction);
        this.options = options;
    }

    public String getCaption() {
        return caption;
    }

    /**
     * Returns an encrypted token combined with answer.
     */
    public String getToken() {
        String id = null == answer ? text : answer;
        return Act.app().crypto().generateToken(id);
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public String getInstruction() {
        return instruction;
    }

    public Set<String> getOptions() {
        return options;
    }

    private String ensureMediaUrl(String url, String text) {
        if (S.isBlank(url)) {
            String encrypted = Act.crypto().encrypt(text);
            return "/~/captcha/" + encrypted;
        }
        return url;
    }
}
