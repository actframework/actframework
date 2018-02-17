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
        this(config.timeFormat());
    }

    @Override
    protected LocalTime parse(DateTimeFormatter formatter, String value) {
        String amended = (isIso && !value.endsWith("Z")) ? value + "Z" : value;
        return formatter.parseLocalTime(amended);
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
        if (null == dateTimePattern) {
            return null;
        }
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
        return dateTimePattern;
    }
}
