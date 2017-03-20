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
import act.data.annotation.Pattern;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgl.$;
import org.osgl.util.AnnotationAware;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JodaDateTimeCodec extends JodaDateTimeCodecBase<DateTime> {

    private DateTimeFormatter dateFormat;

    public JodaDateTimeCodec(DateTimeFormatter dateFormat) {
        this.dateFormat = $.notNull(dateFormat);
        verify();
    }

    public JodaDateTimeCodec(String pattern) {
        if (isIsoStandard(pattern)) {
            dateFormat = ISODateTimeFormat.dateTime();
        } else {
            dateFormat = DateTimeFormat.forPattern(pattern);
        }
        verify();
    }

    @Inject
    public JodaDateTimeCodec(AppConfig config) {
        this(config.dateTimeFormat());
    }

    @Override
    public DateTime resolve(String value) {
        return S.blank(value) ? null : dateFormat.parseDateTime(value);
    }

    @Override
    public Class targetClass() {
        return DateTime.class;
    }

    @Override
    public DateTime parse(String s) {
        return resolve(s);
    }

    @Override
    public String toString(DateTime o) {
        return dateFormat.print(o);
    }

    @Override
    public String toJSONString(DateTime o) {
        String s = toString(o);
        return S.newSizedBuffer(s.length() + 2).append("\"").append(s).append("\"").toString();
    }

    @Override
    public StringValueResolver<DateTime> amended(AnnotationAware beanSpec) {
        Pattern pattern = beanSpec.getAnnotation(Pattern.class);
        return null == pattern ? this : new JodaDateTimeCodec(pattern.value());
    }

    private void verify() {
        DateTime now = DateTime.now();
        String s = toString(now);
        if (!s.equals(toString(parse(s)))) {
            throw new IllegalArgumentException("Invalid date time pattern");
        }
    }
}
