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

import act.data.annotation.Pattern;
import act.util.ActContext;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgl.$;
import org.osgl.util.AnnotationAware;
import org.osgl.util.S;
import org.osgl.util.StringValueResolver;
import org.osgl.util.ValueObject;

public abstract class JodaDateTimeCodecBase<T> extends StringValueResolver<T> implements ValueObject.Codec<T> {

    private DateTimeFormatter formatter;

    public JodaDateTimeCodecBase(DateTimeFormatter formatter) {
        formatter(formatter);
    }

    public JodaDateTimeCodecBase(String pattern) {
        formatter(formatter(pattern));
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
        return S.blank(value) ? null : parse(formatter(), value);
    }

    public final String toJSONString(T o) {
        String s = toString(o);
        return S.newSizedBuffer(s.length() + 2).append("\"").append(s).append("\"").toString();
    }

    @Override
    public final StringValueResolver<T> amended(AnnotationAware beanSpec) {
        Pattern pattern = beanSpec.getAnnotation(Pattern.class);
        return null == pattern ? this : create(pattern.value());
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
        String pattern = ActContext.Base.dataPattern();
        return null == pattern ? this.formatter : formatter(pattern);
    }

    private void formatter(DateTimeFormatter formatter) {
        this.formatter = $.notNull(formatter);
        verify();
    }

    private DateTimeFormatter formatter(String pattern) {
        return isIsoStandard(pattern) ? isoFormatter() : DateTimeFormat.forPattern(pattern);
    }

    public static boolean isIsoStandard(String pattern) {
        return null == pattern || pattern.contains("iso") || pattern.contains("ISO") || pattern.contains("8601");
    }

}
