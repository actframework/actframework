package act.data;

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
import act.util.ActContext;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.mvc.result.BadRequest;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class DateResolverBase<T extends Date> extends StringValueResolver<T> {
    static Logger logger = L.get(DateResolverBase.class);

    private ConcurrentMap<Locale, DateFormat> localizedDateFormats = new ConcurrentHashMap<>();

    protected DateFormat dateFormat;
    protected DateFormat dateFormat2;
    private boolean i18n;
    private Locale defLocale;
    protected AppConfig config;

    public DateResolverBase(AppConfig config) {
        this.i18n = config.i18nEnabled();
        this.config = config;
        this.defLocale = config.locale();
        String pattern = configuredPattern(config);
        if (null == pattern || pattern.contains("8601") || pattern.contains("iso")) {
            this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            this.dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        } else {
            this.dateFormat = new SimpleDateFormat(pattern);
        }
    }

    public DateResolverBase(String pattern) {
        this.dateFormat = new SimpleDateFormat(pattern);
    }

    @Override
    public final T resolve(String value) {
        if (S.blank(value)) {
            return null;
        }
        if ("now".equalsIgnoreCase(value)
                || "today".equalsIgnoreCase(value)
                || "现在".equalsIgnoreCase(value)
                || "今天".equalsIgnoreCase(value)
        ) {
            return cast(new Date());
        }
        // for #691
        Date date = parse(value);
        if (null == date && S.isIntOrLong(value)) {
            long epoc = Long.parseLong(value);
            date = $.convert(epoc).to(Date.class);
            return cast(date);
        }
        if (null == date) {
            throw new BadRequest("Invalid date time format: " + value);
        }
        return cast(date);
    }

    protected String configuredPattern(AppConfig config) {
        return config.dateTimePattern();
    }

    protected abstract T cast(Date date);

    private Date parse(String value) {
        DateFormat dateFormat = dateFormat();
        try {
            return dateFormat.parse(value);
        } catch (ParseException e) {
            if (null == dateFormat2) {
                logger.error("error parsing date value from: %s", value);
                return null;
            }
            try {
                return dateFormat2.parse(value);
            } catch (ParseException e2) {
                logger.error("error parsing date value from: %s", value);
                return null;
            }
        }
    }

    private DateFormat dateFormat() {
        ActContext ctx = ActContext.Base.currentContext();
        String pattern = null == ctx ? null : ctx.dateFormatPattern();
        if (S.notBlank(pattern)) {
            return new SimpleDateFormat(pattern);
        }
        if (!i18n) {
            return dateFormat;
        }
        if (null == ctx) {
            return dateFormat;
        }
        Locale locale = ctx.locale();
        if (null == locale) {
            return dateFormat;
        }
        if (locale.equals(defLocale)) {
            return dateFormat;
        }
        return dateFormat(locale);
    }

    protected DateFormat dateFormat(Locale locale) {
        DateFormat dateFormat = localizedDateFormats.get(locale);
        if (null == dateFormat) {
            String s = localizedPattern(locale);
            dateFormat = new SimpleDateFormat(s);
            localizedDateFormats.putIfAbsent(locale, dateFormat);
        }
        return dateFormat;
    }

    protected String localizedPattern(Locale locale) {
        return config.localizedDateTimePattern(locale);
    }

}
