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
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JodaLocalTimeCodec extends JodaReadablePatialCodecBase<LocalTime> {

    private boolean isIso;

    public JodaLocalTimeCodec(DateTimeFormatter formatter, boolean isIso) {
        super(formatter);
        this.isIso = isIso;
    }

    public JodaLocalTimeCodec(String pattern) {
        super(pattern);
        this.isIso = isIsoStandard(pattern);
    }

    @Inject
    public JodaLocalTimeCodec(AppConfig config) {
        this(config.timePattern());
    }

    @Override
    protected LocalTime parse(DateTimeFormatter formatter, String value) {
        if (formatter == super.formatter) {
            // in the default case we might want to patch the value
            String amended = (isIso && !value.endsWith("Z")) ? value + "Z" : value;
            return formatter.parseLocalTime(amended);
        }
        return formatter.parseLocalTime(value);
    }

    @Override
    protected DateTimeFormatter isoFormatter() {
        return ISODateTimeFormat.timeNoMillis();
    }

    @Override
    protected LocalTime now() {
        return LocalTime.now();
    }

    @Override
    protected JodaDateTimeCodecBase<LocalTime> create(String pattern) {
        return new JodaLocalTimeCodec(pattern);
    }

    @Override
    protected String sanitize(String dateTimePattern) {
        return DateTimeType.TIME.sanitizePattern(dateTimePattern);
    }

    @Override
    protected String dateTimePattern(AppConfig config, Locale locale) {
        return config.localizedTimePattern(locale);
    }
}
