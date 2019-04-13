package act.validation;

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

import act.act_messages;
import act.conf.AppConfig;
import act.i18n.I18n;
import act.util.LogSupportedDestroyableBase;
import org.osgl.util.S;

import java.util.Locale;
import javax.validation.MessageInterpolator;

public class ActValidationMessageInterpolator extends LogSupportedDestroyableBase implements MessageInterpolator {

    private MessageInterpolator realInterpolator;
    private AppConfig config;

    public ActValidationMessageInterpolator(MessageInterpolator defaultInterpolator, AppConfig config) {
        this.realInterpolator = defaultInterpolator;
        this.config = config;
    }

    @Override
    public String interpolate(String messageTemplate, Context context) {
        return interpolate(messageTemplate, context, I18n.locale());
    }

    @Override
    public String interpolate(String messageTemplate, Context context, Locale locale) {
        if (messageTemplate.startsWith("{act.")) {
            return actInterpolate(messageTemplate, locale);
        }
        return realInterpolator.interpolate(messageTemplate, context, locale);
    }

    private String actInterpolate(String messageTemplate, Locale locale) {
        if (null == locale) {
            locale = I18n.locale();
        }
        return I18n.i18n(locale, act_messages.class, messageTemplate);
    }

}
