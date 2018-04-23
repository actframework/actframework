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
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JodaLocalDateTimeCodec extends JodaReadablePatialCodecBase<LocalDateTime> {

    private boolean isIso;

    public JodaLocalDateTimeCodec(DateTimeFormatter formatter, boolean isIso) {
        super(formatter);
        this.isIso = isIso;
    }

    public JodaLocalDateTimeCodec(String pattern) {
        super(pattern);
        isIso = isIsoStandard(pattern);
    }

    @Inject
    public JodaLocalDateTimeCodec(AppConfig config) {
        this(config.dateTimePattern());
    }

    @Override
    protected LocalDateTime parse(DateTimeFormatter formatter, String value) {
        String amended = (isIso && !value.endsWith("Z")) ? value + "Z" : value;
        return formatter.parseLocalDateTime(amended);
    }

    @Override
    protected DateTimeFormatter isoFormatter() {
        return ISODateTimeFormat.dateTimeNoMillis();
    }

    @Override
    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    @Override
    protected JodaDateTimeCodecBase<LocalDateTime> create(String pattern) {
        return new JodaLocalDateTimeCodec(pattern);
    }

    @Override
    protected String dateTimePattern(AppConfig config, Locale locale) {
        return config.localizedDateTimePattern(locale);
    }
}
