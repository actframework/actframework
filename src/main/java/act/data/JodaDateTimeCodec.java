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
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class JodaDateTimeCodec extends JodaReadableInstantCodecBase<DateTime> {

    public JodaDateTimeCodec(DateTimeFormatter formatter) {
        super(formatter);
    }

    public JodaDateTimeCodec(String pattern) {
        super(pattern);
    }

    @Inject
    public JodaDateTimeCodec(AppConfig config) {
        this(config.dateTimePattern());
    }

    @Override
    protected DateTimeFormatter isoFormatter() {
        return ISODateTimeFormat.dateTime();
    }

    @Override
    protected JodaDateTimeCodecBase<DateTime> create(String pattern) {
        return new JodaDateTimeCodec(pattern);
    }

    @Override
    protected DateTime parse(DateTimeFormatter formatter, String value) {
        return formatter.parseDateTime(value);
    }

    @Override
    protected DateTime now() {
        return DateTime.now();
    }

    @Override
    protected String dateTimePattern(AppConfig config, Locale locale) {
        return config.localizedDateTimePattern(locale);
    }

    public static void main(String[] args) {
        Date today;
        String result;
        SimpleDateFormat formatter;

        Locale currentLocale = Locale.TRADITIONAL_CHINESE;
        System.out.println(((SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.CHINA)).toPattern());
    }
}
