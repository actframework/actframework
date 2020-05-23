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

import act.Act;
import act.Destroyable;
import act.conf.AppConfig;
import act.data.annotation.DateFormatPattern;
import act.data.annotation.Pattern;
import act.util.ActContext;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgl.$;
import org.osgl.mvc.result.BadRequest;
import org.osgl.util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.enterprise.context.ApplicationScoped;

public abstract class JodaDateTimeCodecBase<T> extends StringValueResolver<T>
        implements ValueObject.Codec<T>, Destroyable {

    private ConcurrentMap<Locale, DateTimeFormatter> localizedDateFormats = new ConcurrentHashMap<>();
    private boolean i18n;
    private Locale defLocale;
    protected DateTimeFormatter formatter;
    private AppConfig conf;
    private Class<?> dateTimeType;
    private boolean destroyed;

    public JodaDateTimeCodecBase(DateTimeFormatter formatter) {
        exploreDateTimeType();
        conf = Act.appConfig();
        i18n = conf.i18nEnabled();
        defLocale = conf.locale();
        initFormatter(formatter);
    }

    public JodaDateTimeCodecBase(String pattern) {
        exploreDateTimeType();
        E.illegalArgumentIf(S.blank(pattern));
        conf = Act.appConfig();
        i18n = conf.i18nEnabled();
        defLocale = conf.locale();
        initFormatter(formatter(pattern, defLocale));
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }
        if (null != localizedDateFormats) {
            localizedDateFormats.clear();
            localizedDateFormats = null;
        }
        defLocale = null;
        formatter = null;
        conf = null;
        dateTimeType = null;
        releaseResources();
        destroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void releaseResources() {}

    @Override
    public Class<? extends Annotation> scope() {
        return ApplicationScoped.class;
    }

    @Override
    public final T parse(String s) {
        return resolve(s);
    }

    @Override
    public final Class<T> targetClass() {
        return targetType();
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
            return now();
        }
        // for #691
        T retVal = tryParse(value);
        if (null == retVal && S.isIntOrLong(value)) {
            long epoc = Long.parseLong(value);
            return (T) $.convert(epoc).to(dateTimeType);
        }
        if (null == retVal) {
            throw new BadRequest("Invalid date time format: " + value);
        }
        return retVal;
    }

    private T tryParse(String value) {
        try {
            return parse(formatter(), value);
        } catch (Exception e) {
            return null;
        }
    }

    public final String toJSONString(T o) {
        String s = toString(o);
        return S.newSizedBuffer(s.length() + 2).append("\"").append(s).append("\"").toString();
    }

    @Override
    public final StringValueResolver<T> amended(AnnotationAware beanSpec) {
        DateFormatPattern dfp = beanSpec.getAnnotation(DateFormatPattern.class);
        if (null != dfp) {
            return create(dfp.value());
        }
        String format;
        DateFormatPattern pattern = beanSpec.getAnnotation(DateFormatPattern.class);
        if (null == pattern) {
            Pattern patternLegacy = beanSpec.getAnnotation(Pattern.class);
            format = null == patternLegacy ? null : patternLegacy.value();
        } else {
            format = pattern.value();
        }
        return null == format ? this : create(format);
    }

    protected abstract T parse(DateTimeFormatter formatter, String value);

    protected abstract DateTimeFormatter isoFormatter();

    protected abstract T now();

    private void verify() {
        T now = now();
        String s = toString(now);
        if (!s.equals(toString(parse(s)))) {
            throw new IllegalArgumentException("Invalid pattern");
        }
    }

    protected abstract JodaDateTimeCodecBase<T> create(String pattern);

    protected final DateTimeFormatter formatter() {
        ActContext ctx = ActContext.Base.currentContext();
        String pattern = null == ctx ? null : ctx.dateFormatPattern();
        if (S.notBlank(pattern)) {
            String sanitizedPattern = sanitize(pattern);
            if (null != sanitizedPattern) {
                return formatter(sanitizedPattern, ctx.locale(true));
            }
        }
        DateTimeFormatter formatter = defaultFormatter();
        if (!i18n) {
            return formatter;
        }
        if (null == ctx) {
            return formatter;
        }
        Locale locale = ctx.locale();
        if (null == locale) {
            return formatter;
        }
        if (locale.equals(defLocale)) {
            return formatter;
        }
        DateTimeFormatter localizedFormatter = localizedDateFormats.get(locale);
        if (null == localizedFormatter) {
            localizedFormatter = formatter(dateTimePattern(conf, locale), locale);
            localizedDateFormats.putIfAbsent(locale, localizedFormatter);
        }
        return localizedFormatter;
    }

    protected abstract String dateTimePattern(AppConfig config, Locale locale);

    protected String sanitize(String dateTimePattern) {
        return dateTimePattern;
    }

    protected DateTimeFormatter defaultFormatter() {
        return this.formatter;
    }

    private void initFormatter(DateTimeFormatter formatter) {
        this.formatter = $.requireNotNull(formatter);
        verify();
    }

    private void exploreDateTimeType() {
        List<Type> types = Generics.typeParamImplementations(getClass(), StringValueResolver.class);
        dateTimeType = (Class<?>) types.get(0);
    }

    private DateTimeFormatter formatter(String pattern, Locale locale) {
        if (S.blank(pattern)) {
            return defaultFormatter();
        }
        return (isIsoStandard(pattern) ? isoFormatter() : DateTimeFormat.forPattern(pattern)).withLocale(locale);
    }

    public static boolean isIsoStandard(String pattern) {
        return null == pattern || pattern.contains("iso") || pattern.contains("ISO") || pattern.contains("8601");
    }

}
