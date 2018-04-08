package act.util;

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
import act.data.JodaDateTimeCodec;
import act.data.JodaLocalDateCodec;
import act.data.JodaLocalDateTimeCodec;
import act.data.JodaLocalTimeCodec;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.ValueObject;

import java.sql.Time;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * An extension to osgl $ class
 */
public class $$ {

    private static Set<Class> dateTimeTypes = $.cast(C.set(Date.class, java.sql.Date.class, DateTime.class, LocalDate.class, LocalTime.class, LocalDateTime.class, Time.class));

    private static Map<Class, ValueObject.Codec> codecs = $.cast(C.Map(
            DateTime.class, Act.getInstance(JodaDateTimeCodec.class),
            LocalDateTime.class, Act.getInstance(JodaLocalDateTimeCodec.class),
            LocalDate.class, Act.getInstance(JodaLocalDateCodec.class),
            LocalTime.class, Act.getInstance(JodaLocalTimeCodec.class)
    ));

    public static boolean isDateTimeType(Class<?> type) {
        return dateTimeTypes.contains(type);
    }

    public static String toString(Object v, boolean isDateTimeType, boolean isArray) {
        if (isArray) {
            return $.toString2(v);
        } else if (isDateTimeType) {
            ValueObject.Codec codec = codecs.get(v.getClass());
            return null == codec ? v.toString() : codec.toString(v);
        }
        return v.toString();
    }

}
