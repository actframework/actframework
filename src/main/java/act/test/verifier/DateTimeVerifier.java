package act.test.verifier;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2018 ActFramework
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
import org.joda.time.*;
import org.joda.time.format.*;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;

import java.text.DateFormat;
import java.util.Date;

public abstract class DateTimeVerifier extends Verifier {

    protected long timestamp;

    @Override
    public void init(Object param) {
        super.init(param);
        timestamp = convert($.requireNotNull(param));
    }

    @Override
    public boolean verify(Object value) {
        if (null == value) {
            return false;
        }
        return verify(timestamp, convert(value));
    }

    protected abstract boolean verify(long expected, long found);

    private Long convert(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Date) {
            return ((Date) value).getTime();
        } else if (value instanceof ReadableDateTime) {
            return ((ReadableDateTime) value).getMillis();
        } else if (value instanceof ReadableInstant) {
            return ((ReadableInstant) value).getMillis();
        } else if (value instanceof LocalDate) {
            return ((LocalDate) value).toDate().getTime();
        } else {
            return convert(S.string(value));
        }
    }

    private long convert(String param) {
        if (S.isInt(param)) {
            return Long.parseLong(param);
        }
        App app = Act.app();
        if (null != app) {
            AppConfig config = app.config();
            Long l = tryWithAppConfig(param, config);
            if (null != l) {
                return l;
            }
        }
        Long l = tryWithDefaultDateTimeFormats(param);
        E.unexpectedIf(null == l, "Unknown date time string: " + param);
        return l;
    }

    private Long tryWithAppConfig(String s, AppConfig config) {
        Long l = tryWithFormat(s, config.dateTimeFormat());
        if (null != l) {
            return l;
        }
        return tryWithFormat(s, config.dateFormat());
    }

    public static Long tryWithDefaultDateTimeFormats(String s) {
        return tryWithFormat(s, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'","yyyy-MM-dd hh:mm:ss", "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "yyyyMMdd HH:mm:ss",
                "yyyyMMdd",
                "dd/MMM/yyyy",
                "dd-MMM-yyyy",
                "dd MMM yyyy");
    }

    private static Long tryWithFormat(String s, String pattern, String... otherPatterns) {
        DateTimeFormatter format = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendTimeZoneOffset("Z", true, 2, 4)
                .toFormatter();
        Long l = tryWithFormat(s, format);
        if (null != l) {
            return l;
        }
        l = tryWithFormat(s, DateTimeFormat.forPattern(pattern));
        if (null != l) {
            return l;
        }
        for (String op : otherPatterns) {
            l = tryWithFormat(s, DateTimeFormat.forPattern(op));
            if (null != l) {
                return l;
            }
        }
        return null;
    }

    private static Long tryWithFormat(String s, DateFormat pattern) {
        try {
            Date dt = pattern.parse(s);
            return dt.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private static Long tryWithFormat(String s, DateTimeFormatter format) {
        try {
            DateTime dt = format.parseDateTime(s);
            return dt.getMillis();
        } catch (Exception e) {
            return null;
        }
    }

}
