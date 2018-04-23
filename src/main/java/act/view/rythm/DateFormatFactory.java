package act.view.rythm;

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
import act.conf.AppConfig;
import act.util.ActContext;
import org.rythmengine.extension.IDateFormatFactory;
import org.rythmengine.template.ITemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DateFormatFactory implements IDateFormatFactory {

    private ConcurrentMap<DateFormatKey, DateFormat> cache = new ConcurrentHashMap<>();
    private AppConfig config;

    public DateFormatFactory(AppConfig config) {
        this.config = config;
    }

    @Override
    public DateFormat createDateFormat(ITemplate template, String pattern, Locale locale, String timezone) {
        locale = ensureLocale(locale, template);
        DateFormat df;
        DateFormatKey key = new DateFormatKey(locale, timezone, pattern);
        df = cache.get(key);
        if (null == df) {
            if (null == pattern) {
                pattern = config.localizedDateTimePattern(locale);
            }
            df = new SimpleDateFormat(pattern, locale);
            if (null != timezone) {
                df.setTimeZone(TimeZone.getTimeZone(timezone));
            }
            cache.putIfAbsent(key, df);
        }
        return df;
    }

    private Locale ensureLocale(Locale locale, ITemplate template) {
        if (null != locale) {
            return locale;
        }
        if (null != template) {
            locale = template.__curLocale();
        }
        if (null == locale) {
            ActContext ctx = ActContext.Base.currentContext();
            return null == ctx ? Act.appConfig().locale() : ctx.locale(true);
        }
        return locale;
    }
}
