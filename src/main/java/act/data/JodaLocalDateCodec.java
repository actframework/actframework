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
import org.joda.time.LocalDate;
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
public class JodaLocalDateCodec extends JodaDateTimeCodecBase<LocalDate> {

    private DateTimeFormatter dateFormat;

    public JodaLocalDateCodec(DateTimeFormatter dateFormat) {
        this.dateFormat = $.notNull(dateFormat);
        verify();
    }

    public JodaLocalDateCodec(String pattern) {
        if (isIsoStandard(pattern)) {
            dateFormat = ISODateTimeFormat.date();
        } else {
            dateFormat = DateTimeFormat.forPattern(pattern);
        }
        verify();
    }

    @Inject
    public JodaLocalDateCodec(AppConfig config) {
        this(config.dateFormat());
    }

    @Override
    public LocalDate resolve(String value) {
        return S.blank(value) ? null : dateFormat.parseLocalDate(value);
    }

    @Override
    public LocalDate parse(String s) {
        return resolve(s);
    }

    @Override
    public String toString(LocalDate localDate) {
        return dateFormat.print(localDate);
    }

    @Override
    public String toJSONString(LocalDate localDate) {
        return null;
    }

    @Override
    public StringValueResolver<LocalDate> amended(AnnotationAware beanSpec) {
        Pattern pattern = beanSpec.getAnnotation(Pattern.class);
        return null == pattern ? this : new JodaLocalDateCodec(pattern.value());
    }

    private void verify() {
        LocalDate now = LocalDate.now();
        String s = toString(now);
        if (!s.equals(toString(parse(s)))) {
            throw new IllegalArgumentException("Invalid date time pattern");
        }
    }
}
