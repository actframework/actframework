package act.view.rythm;

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

import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.rythmengine.extension.Transformer;
import org.rythmengine.template.ITemplate;

import java.util.Locale;

public class JodaTransformers {

    static String format(ReadableInstant dateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern);
        return formatter.print(dateTime);
    }

    static String format(ReadablePartial dateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern);
        return formatter.print(dateTime);
    }

    @Transformer(requireTemplate = true)
    public static String shortStyle(DateTime dateTime) {
        return shortStyle(null, dateTime);
    }

    public static String shortStyle(ITemplate template, DateTime dateTime) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("SS", locale));
    }

    @Transformer(requireTemplate = true)
    public static String mediumStyle(DateTime dateTime) {
        return mediumStyle(null, dateTime);
    }

    public static String mediumStyle(ITemplate template, DateTime dateTime) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("MM", locale));
    }

    @Transformer(requireTemplate = true)
    public static String longStyle(DateTime dateTime) {
        return longStyle(null, dateTime);
    }

    public static String longStyle(ITemplate template, DateTime dateTime) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("LL", locale));
    }

    @Transformer(requireTemplate = true)
    public static String shortStyle(LocalDateTime dateTime) {
        return shortStyle(null, dateTime);
    }

    public static String shortStyle(ITemplate template, LocalDateTime dateTime) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("SS", locale));
    }

    @Transformer(requireTemplate = true)
    public static String mediumStyle(LocalDateTime dateTime) {
        return mediumStyle(null, dateTime);
    }

    public static String mediumStyle(ITemplate template, LocalDateTime dateTime) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("MM", locale));
    }

    @Transformer(requireTemplate = true)
    public static String longStyle(LocalDateTime dateTime) {
        return longStyle(null, dateTime);
    }

    public static String longStyle(ITemplate template, LocalDateTime dateTime) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(dateTime, DateTimeFormat.patternForStyle("LL", locale));
    }

    @Transformer(requireTemplate = true)
    public static String shortStyle(LocalDate LocalDate) {
        return shortStyle(null, LocalDate);
    }

    public static String shortStyle(ITemplate template, LocalDate LocalDate) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(LocalDate, DateTimeFormat.patternForStyle("S-", locale));
    }

    @Transformer(requireTemplate = true)
    public static String mediumStyle(LocalDate LocalDate) {
        return mediumStyle(null, LocalDate);
    }

    public static String mediumStyle(ITemplate template, LocalDate LocalDate) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(LocalDate, DateTimeFormat.patternForStyle("M-", locale));
    }

    @Transformer(requireTemplate = true)
    public static String longStyle(LocalDate LocalDate) {
        return longStyle(null, LocalDate);
    }

    public static String longStyle(ITemplate template, LocalDate LocalDate) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(LocalDate, DateTimeFormat.patternForStyle("L-", locale));
    }


    @Transformer(requireTemplate = true)
    public static String shortStyle(LocalTime LocalTime) {
        return shortStyle(null, LocalTime);
    }

    public static String shortStyle(ITemplate template, LocalTime LocalTime) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(LocalTime, DateTimeFormat.patternForStyle("-S", locale));
    }

    @Transformer(requireTemplate = true)
    public static String mediumStyle(LocalTime LocalTime) {
        return mediumStyle(null, LocalTime);
    }

    public static String mediumStyle(ITemplate template, LocalTime LocalTime) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(LocalTime, DateTimeFormat.patternForStyle("-M", locale));
    }

    @Transformer(requireTemplate = true)
    public static String longStyle(LocalTime LocalTime) {
        return longStyle(null, LocalTime);
    }

    public static String longStyle(ITemplate template, LocalTime LocalTime) {
        Locale locale = null == template ? Locale.getDefault() : template.__curLocale();
        return format(LocalTime, DateTimeFormat.patternForStyle("-L", locale));
    }

}
