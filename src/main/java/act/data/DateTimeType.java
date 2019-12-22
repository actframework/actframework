package act.data;

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
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgl.util.E;
import org.osgl.util.S;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public enum DateTimeType {
    DATE() {
        @Override
        public String defaultPattern(AppConfig config) {
            return config.datePattern();
        }

        @Override
        public String defaultPattern(AppConfig config, Locale locale) {
            return config.localizedDatePattern(locale);
        }

        @Override
        protected String defaultLongPattern(Locale locale) {
            return ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.LONG, locale)).toPattern();
        }

        @Override
        protected String defaultMediumPattern(Locale locale) {
            return ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM, locale)).toPattern();
        }

        @Override
        protected String defaultShortPattern(Locale locale) {
            return ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, locale)).toPattern();
        }
    },

    TIME() {
        @Override
        public String defaultPattern(AppConfig config) {
            return config.timePattern();
        }

        @Override
        public String defaultPattern(AppConfig config, Locale locale) {
            return config.localizedTimePattern(locale);
        }

        @Override
        protected String defaultLongPattern(Locale locale) {
            return ((SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.LONG, locale)).toPattern();
        }

        @Override
        protected String defaultMediumPattern(Locale locale) {
            return ((SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.MEDIUM, locale)).toPattern();
        }

        @Override
        protected String defaultShortPattern(Locale locale) {
            return ((SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT, locale)).toPattern();
        }
    },

    DATE_TIME() {
        @Override
        public String defaultPattern(AppConfig config) {
            return config.dateTimePattern();
        }

        @Override
        public String defaultPattern(AppConfig config, Locale locale) {
            return config.localizedDateTimePattern(locale);
        }

        @Override
        protected String defaultLongPattern(Locale locale) {
            return ((SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale)).toPattern();
        }

        @Override
        protected String defaultMediumPattern(Locale locale) {
            return ((SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale)).toPattern();
        }

        @Override
        protected String defaultShortPattern(Locale locale) {
            return ((SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)).toPattern();
        }
    };

    private volatile DateTimeFormatter defaultJodaFormatter;

    public String suffix() {
        if (DATE == this) {
            return ".date";
        } else if (TIME == this) {
            return ".time";
        } else {
            return ".date_time";
        }
    }

    public DateTimeFormatter defaultJodaFormatter() {
        if (null == defaultJodaFormatter) {
            synchronized (this) {
                if (null == defaultJodaFormatter)
                    defaultJodaFormatter = createDefaultJodaFormatter(Act.appConfig());
            }
        }
        return defaultJodaFormatter;
    }

    public String defaultPattern(DateTimeStyle style, Locale locale) {
        int styleId = null == style ? DateFormat.DEFAULT : style.id();
        if (DATE == this) {
            return ((SimpleDateFormat) DateFormat.getDateInstance(styleId, locale)).toPattern();
        } else if (TIME == this) {
            return ((SimpleDateFormat) DateFormat.getTimeInstance(styleId, locale)).toPattern();
        } else {
            return ((SimpleDateFormat) DateFormat.getDateTimeInstance(styleId, styleId, locale)).toPattern();
        }
    }

    public String sanitizePattern(String dateTimePattern) {
        if (S.blank(dateTimePattern)) {
            return null;
        }
        if (DATE == this) {
            // ensure no time part
            if (dateTimePattern.contains("h")
                    || dateTimePattern.contains("H")
                    || dateTimePattern.contains("m")
                    || dateTimePattern.contains("s")
                    || dateTimePattern.contains("S")) {
                return null;
            }
        } else if (TIME == this) {
            // ensure no date part
            if (dateTimePattern.contains("M")
                    || dateTimePattern.contains("y")
                    || dateTimePattern.contains("d")
                    || dateTimePattern.contains("Y")
                    || dateTimePattern.contains("c")
                    || dateTimePattern.contains("e")
                    || dateTimePattern.contains("x")
                    || dateTimePattern.contains("C")
                    || dateTimePattern.contains("D")
                    || dateTimePattern.contains("F")
                    || dateTimePattern.contains("w")
                    || dateTimePattern.contains("W")
                    || dateTimePattern.contains("k")
                    || dateTimePattern.contains("K")) {
                return null;
            }
        }
        return dateTimePattern;
    }

    public abstract String defaultPattern(AppConfig config, Locale locale);

    public abstract String defaultPattern(AppConfig config);

    protected abstract String defaultLongPattern(Locale locale);
    protected abstract String defaultMediumPattern(Locale locale);
    protected abstract String defaultShortPattern(Locale locale);

    protected DateTimeFormatter createDefaultJodaFormatter(AppConfig config) {
        String pattern = defaultPattern(config);
        return DateTimeFormat.forPattern(pattern).withLocale(config.locale());
    }

    public static DateTimeType of(ReadablePartial partial) {
        E.NPE(partial);
        if (partial instanceof LocalDate) {
            return DATE;
        }
        if (partial instanceof LocalTime) {
            return TIME;
        }
        if (partial instanceof LocalDateTime) {
            return DATE_TIME;
        }
        throw E.unsupport("ReadablePartial not supported: " + partial.getClass());
    }
}
