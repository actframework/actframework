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
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JodaLocalDateCodec extends JodaReadablePatialCodecBase<LocalDate> {

    public JodaLocalDateCodec(DateTimeFormatter formatter) {
        super(formatter);
    }

    public JodaLocalDateCodec(String pattern) {
        super(pattern);
    }

    @Inject
    public JodaLocalDateCodec(AppConfig config) {
        this(config.datePattern());
    }

    @Override
    protected JodaDateTimeCodecBase<LocalDate> create(String pattern) {
        return new JodaLocalDateCodec(pattern);
    }

    @Override
    protected LocalDate parse(DateTimeFormatter formatter, String value) {
        return formatter.parseLocalDate(value);
    }

    @Override
    protected DateTimeFormatter isoFormatter() {
        return ISODateTimeFormat.localDateParser();
    }

    @Override
    protected LocalDate now() {
        return LocalDate.now();
    }

    @Override
    protected String sanitize(String dateTimePattern) {
        return DateTimeType.DATE.sanitizePattern(dateTimePattern);
    }

    @Override
    protected String dateTimePattern(AppConfig config, Locale locale) {
        return config.localizedDatePattern(locale);
    }
}
