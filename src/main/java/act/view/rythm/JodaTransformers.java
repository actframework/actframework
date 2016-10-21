package act.view.rythm;

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
