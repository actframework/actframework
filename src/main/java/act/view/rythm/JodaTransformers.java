package act.view.rythm;

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
import act.app.App;
import act.conf.AppConfig;
import act.data.DateTimeType;
import act.util.ActContext;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgl.$;
import org.osgl.util.S;
import org.rythmengine.extension.Transformer;
import org.rythmengine.template.ITemplate;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

public class JodaTransformers {

    private static class Key {
        private DateTimeType type;
        private Locale locale;
        private String timezone;
        private String pattern;

        public Key(DateTimeType type, Locale locale, String timezone, String pattern) {
            this.type = type;
            this.locale = locale;
            this.timezone = timezone;
            this.pattern = pattern;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return type == key.type &&
                    Objects.equals(pattern, key.pattern) &&
                    Objects.equals(locale, key.locale) &&
                    Objects.equals(timezone, key.timezone);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, locale, timezone, pattern);
        }
    }

    public static void classInit(App app) {
        formatters = app.createConcurrentMap();
    }

    private static ConcurrentMap<Key, DateTimeFormatter> formatters;

    private static DateTimeFormatter formatter(DateTimeType type, String pattern, Locale locale, String timezone) {
        Key key = new Key(type, locale, timezone, pattern);
        DateTimeFormatter formatter = formatters.get(key);
        if (null == formatter) {
            formatter = type.defaultJodaFormatter();
            AppConfig config = Act.appConfig();
            if (null == locale) {
                ActContext ctx = ActContext.Base.currentContext();
                locale = null == ctx ? config.locale() : ctx.locale(true);
            }
            if (config.i18nEnabled() && $.ne(locale, config.locale())) {
                if (S.blank(pattern)) {
                    pattern = type.defaultPattern(config, locale);
                }
                formatter = DateTimeFormat.forPattern(pattern).withLocale(locale);
                if (S.notBlank(timezone)) {
                    formatter = formatter.withZone(DateTimeZone.forID(timezone));
                }
            }
            formatters.putIfAbsent(key, formatter);
        }
        return formatter;
    }

    static String format(ReadableInstant dateTime, String pattern, Locale locale, String timezone) {
        DateTimeFormatter formatter = formatter(DateTimeType.DATE_TIME, pattern, locale, timezone);
        return formatter.print(dateTime);
    }

    static String format(ReadablePartial dateTime, String pattern, Locale locale, String timezone) {
        DateTimeType type = DateTimeType.of(dateTime);
        DateTimeFormatter formatter = formatter(type, pattern, locale, timezone);
        return formatter.print(dateTime);
    }

    @Transformer(requireTemplate = true)
    public static String shortStyle(DateTime dateTime) {
        return shortStyle(null, dateTime);
    }

    public static String shortStyle(ITemplate template, DateTime dateTime) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("SS", locale), locale, null);
    }

    @Transformer(requireTemplate = true)
    public static String mediumStyle(DateTime dateTime) {
        return mediumStyle(null, dateTime);
    }

    public static String mediumStyle(ITemplate template, DateTime dateTime) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("MM", locale), locale, null);
    }

    @Transformer(requireTemplate = true)
    public static String longStyle(DateTime dateTime) {
        return longStyle(null, dateTime);
    }

    public static String longStyle(ITemplate template, DateTime dateTime) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("LL", locale), locale, null);
    }

    @Transformer(requireTemplate = true)
    public static String shortStyle(LocalDateTime dateTime) {
        return shortStyle(null, dateTime);
    }

    public static String shortStyle(ITemplate template, LocalDateTime dateTime) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("SS", locale), locale, null);
    }

    @Transformer(requireTemplate = true)
    public static String mediumStyle(LocalDateTime dateTime) {
        return mediumStyle(null, dateTime);
    }

    public static String mediumStyle(ITemplate template, LocalDateTime dateTime) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("MM", locale), locale, null);
    }

    @Transformer(requireTemplate = true)
    public static String longStyle(LocalDateTime dateTime) {
        return longStyle(null, dateTime);
    }

    public static String longStyle(ITemplate template, LocalDateTime dateTime) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("LL", locale), locale, null);
    }

    @Transformer(requireTemplate = true)
    public static String shortStyle(LocalDate LocalDate) {
        return shortStyle(null, LocalDate);
    }

    public static String shortStyle(ITemplate template, LocalDate LocalDate) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(LocalDate, DateTimeFormat.patternForStyle("S-", locale), locale, null);
    }

    @Transformer(requireTemplate = true)
    public static String mediumStyle(LocalDate LocalDate) {
        return mediumStyle(null, LocalDate);
    }

    public static String mediumStyle(ITemplate template, LocalDate LocalDate) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(LocalDate, DateTimeFormat.patternForStyle("M-", locale), locale, null);
    }

    @Transformer(requireTemplate = true)
    public static String longStyle(LocalDate LocalDate) {
        return longStyle(null, LocalDate);
    }

    public static String longStyle(ITemplate template, LocalDate LocalDate) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(LocalDate, DateTimeFormat.patternForStyle("L-", locale), locale, null);
    }


    @Transformer(requireTemplate = true)
    public static String shortStyle(LocalTime LocalTime) {
        return shortStyle(null, LocalTime);
    }

    public static String shortStyle(ITemplate template, LocalTime LocalTime) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(LocalTime, DateTimeFormat.patternForStyle("-S", locale), locale, null);
    }

    @Transformer(requireTemplate = true)
    public static String mediumStyle(LocalTime LocalTime) {
        return mediumStyle(null, LocalTime);
    }

    public static String mediumStyle(ITemplate template, LocalTime LocalTime) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(LocalTime, DateTimeFormat.patternForStyle("-M", locale), locale, null);
    }

    @Transformer(requireTemplate = true)
    public static String longStyle(LocalTime LocalTime) {
        return longStyle(null, LocalTime);
    }

    public static String longStyle(ITemplate template, LocalTime LocalTime) {
        Locale locale = null == template ? Act.appConfig().locale() : template.__curLocale();
        return format(LocalTime, DateTimeFormat.patternForStyle("-L", locale), locale, null);
    }

}
