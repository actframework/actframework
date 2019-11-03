package act.test.func;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2018 ActFramework
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

import act.Act;
import act.test.inbox.Inbox;
import act.test.util.NamedLogic;
import act.test.verifier.DateTimeVerifier;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.util.*;
import org.osgl.util.converter.TypeConverterRegistry;
import org.rythmengine.utils.Time;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.Collection;
import java.util.List;

public abstract class Func extends NamedLogic {

    @Override
    protected Class<? extends NamedLogic> type() {
        return Func.class;
    }

    public abstract Object apply();

    public static class VerifiableEmail extends Func {
        @Override
        public Object apply() {
            String email = Act.getInstance(Inbox.class).account;
            S.Pair pair = S.binarySplit(email, '@');
            String username = pair.left();
            return S.concat(username, "+", S.shortUrlSafeRandom(), "@", pair.right());
        }

        @Override
        protected List<String> aliases() {
            return C.list("checkableEmail");
        }
    }

    public static class SizeOf extends Func {

        int size;

        @Override
        public void init(Object param) {
            if (param instanceof Collection) {
                Collection collection = $.cast(param);
                size = collection.size();
            } else if (param instanceof CharSequence) {
                CharSequence charSequence = $.cast(param);
                size = charSequence.length();
            } else if (param.getClass().isArray()) {
                size = Array.getLength(param);
            } else {
                throw new UnsupportedOperationException("SizeOf cannot be applied to " + param.getClass());
            }
        }

        @Override
        public Object apply() {
            return size;
        }
    }

    public static abstract class _Date extends Func {

        private DateTimeFormatter fmt;

        protected abstract LocalDate getDate();

        @Override
        public void init(Object param) {
            String pattern = S.string(param);
            fmt = DateTimeFormat.forPattern(pattern);
        }

        @Override
        public Object apply() {
            LocalDate date = getDate();
            return null == fmt ? date : fmt.print(date);
        }
    }

    public static class Today extends _Date {
        @Override
        public LocalDate getDate() {
            return LocalDate.now();
        }
    }

    public static class Tomorrow extends _Date {
        @Override
        public LocalDate getDate() {
            return LocalDate.now().plusDays(1);
        }
    }

    public static class Yesterday extends _Date {
        @Override
        public LocalDate getDate() {
            return LocalDate.now().minusDays(1);
        }
    }

    public static class GetTime extends Func {

        private Integer deltaInSeconds;
        private DateTime dateTime;

        @Override
        public void init(Object param) {
            super.init(param);
            if (param instanceof String) {
                initStr((String) param);
            } else if (param instanceof List) {
                initList((List<String>) param);
            }
        }

        private void initStr(String s) {
            E.illegalArgumentIf(!tryParseDuration(s) && !tryParseDateTime(s), "Invalid GetTime argument: " + s);
        }

        private void initList(List<String> list) {
            E.illegalArgumentIf(list.isEmpty());
            String first = list.get(0);
            if (list.size() < 2) {
                initStr(first);
                return;
            }
            String second = list.get(1);
            if (!tryParseDateTime(first)) {
                if (!tryParseDuration(first)) {
                    throw new IllegalArgumentException("Invalid GetTime argument: " + list);
                } else if (!tryParseDateTime(second)) {
                    throw new IllegalArgumentException("Invalid GetTime argument: " + list);
                }
            } else if (!tryParseDuration(second)) {
                throw new IllegalArgumentException("Invalid GetTime argument: " + list);
            }
        }

        private boolean tryParseDuration(String s) {
            boolean negative = false;
            if (s.startsWith("-")) {
                negative = true;
                s = s.substring(1);
            } else if (s.startsWith("+")) {
                s = s.substring(1);
            }
            try {
                int n = Time.parseDuration(s);
                deltaInSeconds = negative ? -1 * n : n;
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        private boolean tryParseDateTime(String s) {
            Long l = DateTimeVerifier.tryWithDefaultDateTimeFormats(s);
            if (null == l) {
                return false;
            }
            dateTime = new DateTime(l);
            return true;
        }

        @Override
        public Object apply() {
            DateTime now = null == dateTime ? DateTime.now() : dateTime;
            if (null != deltaInSeconds) {
                int delta = deltaInSeconds;
                now = delta < 0 ? now.minusSeconds(-1 * delta) : now.plusSeconds(delta);
            }
            return now;
        }
    }

    /**
     * `_Time` is the base class for all time relevant functions, including
     * `Now`, `NextMinute`, `NextHour`.
     *
     * Examples of configuring `_Time`:
     *
     * `${now(+1d)}` - now plus one day
     * `${now(-2h)}` - now minus 2 hours
     * `${now(true,+3s)} - now with high precision plus 3 seconds
     * `${now(-5mn,lp)} - now with low precision minus 5 minutes
     */
    public abstract static class _Time extends Func {

        /**
         * When `highPrecision` is true, then it set the precision to
         * milliseconds instead of second.
         *
         * The default value is `false`.
         */
        protected boolean highPrecision = false;


        protected int delta;

        @Override
        public void init(Object param) {
            if (param.getClass().isArray()) {
                int n = Array.getLength(param);
                for (int i = 0; i < n; ++i) {
                    Object o = Array.get(param, i);
                    parseParam(o);
                }
            } else if (param instanceof Collection) {
                for (Object o : ((Collection) param)) {
                    parseParam(o);
                }
            } else {
                parseParam(param);
            }
        }

        private void parseParam(Object o) {
            String s = S.string(o);
            String[] sa = s.split(S.COMMON_SEP);
            for (String si : sa) {
                parseParamPart(si);
            }
        }

        private void parseParamPart(String s) {
            if ("true".equalsIgnoreCase(s)) {
                highPrecision = true;
            } else if ("false".equalsIgnoreCase(s)) {
                highPrecision = false;
            } else if (Keyword.of("highPrecision").equals(Keyword.of(s))) {
                highPrecision = true;
            } else if (Keyword.of("lowPrecision").equals(Keyword.of(s))) {
                highPrecision = false;
            } else if ("hp".equalsIgnoreCase(s)) {
                highPrecision = true;
            } else if ("lp".equalsIgnoreCase(s)) {
                highPrecision = false;
            } else if (s.startsWith("+")) {
                s = s.substring(1);
                delta = Time.parseDuration(s);
            } else {
                if (S.isInt(s)) {
                    delta = Integer.parseInt(s);
                } else if (s.startsWith("-")) {
                    s = s.substring(1);
                    delta = -Time.parseDuration(s);
                } else {
                    throw new UnexpectedException("Unknown time parameter: " + s);
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    public static class Now extends _Time {
        @Override
        public Object apply() {
            return now();
        }

        protected DateTime now() {
            DateTime dt = highPrecision ? DateTime.now() : DateTime.now().withMillisOfSecond(0);
            return 0 == delta ? dt : dt.plusSeconds(delta);
        }
    }

    public static class NextMinute extends Now {
        @Override
        public Object apply() {
            return now().plusMinutes(1);
        }
    }

    public static class NextHour extends Now {
        @Override
        public Object apply() {
            return now().plusHours(1);
        }
    }

    public static class AfterLast extends Func {

        private String retVal;

        @Override
        public void init(Object param) {
            E.illegalArgumentIfNot(param instanceof List, "2 parameters expected for afterLast function");
            List<String> list = (List) param;
            E.illegalArgumentIfNot(list.size() == 2, "2 parameters expected for afterLast function");
            String targetStr = list.get(0);
            String search = list.get(1);
            retVal = S.cut(targetStr).afterLast(search);
        }

        @Override
        public Object apply() {
            return retVal;
        }

        @Override
        protected List<String> aliases() {
            return C.list("after");
        }
    }

    public static class BeforeFirst extends Func {

        private String retVal;

        @Override
        public void init(Object param) {
            E.illegalArgumentIfNot(param instanceof List, "2 parameters expected for beforeFirst function");
            List<String> list = (List) param;
            E.illegalArgumentIfNot(list.size() == 2, "2 parameters expected for beforeFirst function");
            String targetStr = list.get(0);
            String search = list.get(1);
            retVal = S.cut(targetStr).beforeFirst(search);
        }

        @Override
        public Object apply() {
            return retVal;
        }

        @Override
        protected List<String> aliases() {
            return C.list("before");
        }
    }


    /**
     * Apply subString function to given string.
     *
     * If one param is provided it must be an integer which specify the
     * begin index of the substr.
     *
     * If two parameters are provided, the second one specify the end
     * index (exclusive) of the substr.
     *
     * @see String#substring(int)
     * @see String#substring(int, int)
     */
    public static class SubStr extends Func {

        private String targetStr;

        @Override
        public void init(Object param) {
            E.illegalArgumentIfNot(param instanceof List, "At least 2 parameters expected for subStr function");
            List<String> params = (List<String>) param;
            targetStr = S.ensure(params.get(0)).strippedOff(S.DOUBLE_QUOTES);
            String sBegin = params.get(1);
            E.illegalArgumentIfNot(S.isInt(sBegin), "the 2nd parameter must be valid integer");
            int begin = Integer.parseInt(sBegin);
            E.illegalArgumentIf(begin < 0, "the 2nd parameter must be valid integer");
            int end = -1;
            if (params.size() > 2) {
                String sEnd = params.get(2);
                E.illegalArgumentIfNot(S.isInt(sEnd), "the 3nd parameter must be valid integer");
                end = Integer.parseInt(sEnd);
                E.illegalArgumentIf(end < begin, "the 3nd parameter not be less than the 2nd parameter");
                if (end > targetStr.length()) {
                    end = targetStr.length();
                }
            }
            targetStr = -1 == end ? targetStr.substring(begin) : targetStr.substring(begin, end);
        }

        @Override
        public Object apply() {
            return targetStr;
        }

        @Override
        protected List<String> aliases() {
            return C.list("subString", "substr");
        }
    }

    /**
     * Random pick up from a list of parameters.
     */
    public static class RandomOf extends Func {

        private boolean isList;
        private List list;

        @Override
        public void init(Object param) {
            super.init(param);
            isList = param instanceof List;
            if (isList) {
                list = (List) param;
            }
        }

        @Override
        public Object apply() {
            return isList ? $.random(list) : initVal;
        }

        @Override
        protected List<String> aliases() {
            return C.list("randOf", "randSelect", "randomSelect", "pickOne");
        }
    }

    /**
     * Generate random string.
     *
     * If initVal is provided then it must be a positive integer which indicate
     * the length of the random string. Otherwise the length will be any
     * where between 5 and 15.
     */
    public static class RandomStr extends Func {
        @Override
        public Object apply() {
            int length = 0;
            if (null != initVal) {
                try {
                    length = $.convert(initVal).toInt();
                } catch (Exception e) {
                    warn(e, "RandomStr func init value (max length) shall be evaluated to an integer, found: " + initVal);
                }
            }
            if (length < 1) {
                length = 5 + N.randInt(10);
            }
            return S.urlSafeRandom(length);
        }

        @Override
        protected List<String> aliases() {
            return C.list("randStr", "randomString", "randString");
        }
    }

    /**
     * Generate random int value.
     *
     * If initVal is provided then
     * - if there is 1 init val, it specify the ceiling of the random integer
     * - if there are 2 values, the first is the bottom of the random val and the second is the ceiling of the val
     */
    public static class RandomInt extends Func {
        @Override
        public Object apply() {
            int max = 0;
            boolean positive = true;
            int min = 0;
            if (null != initVal) {
                Object ceilling = initVal;
                if (initVal instanceof List) {
                    List list = (List) initVal;
                    Object bottom = list.get(0);
                    min = $.convert(bottom).toInt();
                    ceilling = list.get(1);
                }
                try {
                    max = $.convert(ceilling).toInt();
                    if (max < 0) {
                        positive = false;
                        if (max > min) {
                            int tmp = min;
                            min = max;
                            max = tmp;
                        }
                        max = min - max;
                    } else {
                        if (max < min) {
                            int tmp = min;
                            min = max;
                            max = tmp;
                        }
                        max = max - min;
                    }
                } catch (Exception e) {
                    warn(e, "RandomInt func init value (max) shall be evaluated to an integer, found: " + initVal);
                }
            }
            if (max == 0) {
                max = 100;
            }
            int retVal = N.randInt(max);
            if (!positive) {
                retVal = -retVal;
            }
            retVal += min;
            return retVal;
        }

        @Override
        protected List<String> aliases() {
            return C.list("randInt", "randomInteger", "randInteger");
        }
    }


    /**
     * Generate random double value.
     *
     * If initVal is provided then
     * - if there is 1 init val, it specify the ceiling of the random integer
     * - if there are 2 values, the first is the bottom of the random val and the second is the ceiling of the val
     */
    public static class RandomDouble extends Func {
        @Override
        public Object apply() {
            int max = 0;
            boolean positive = true;
            int min = 0;
            if (null != initVal) {
                Object ceilling = initVal;
                if (initVal instanceof List) {
                    List list = (List) initVal;
                    Object bottom = list.get(0);
                    min = $.convert(bottom).toInt();
                    ceilling = list.get(1);
                }
                try {
                    max = $.convert(ceilling).toInt();
                    if (max < 0) {
                        positive = false;
                        if (max > min) {
                            int tmp = min;
                            min = max;
                            max = tmp;
                        }
                        max = min - max;
                    } else {
                        if (max < min) {
                            int tmp = min;
                            min = max;
                            max = tmp;
                        }
                        max = max - min;
                    }
                } catch (Exception e) {
                    warn(e, "RandomDouble func init value (max) shall be evaluated to an integer, found: " + initVal);
                }
            }
            if (max == 0) {
                max = 100;
            }
            double retVal = N.randDouble();
            if (!positive) {
                retVal = -retVal;
            }
            if (retVal < min) {
                retVal = retVal + min;
            }
            if (retVal > max) {
                retVal = retVal - (max - min);
            }
            return retVal;
        }

        @Override
        protected List<String> aliases() {
            return C.list("randDbl", "randDouble", "randomDbl");
        }
    }

    /**
     * Generate random double value.
     *
     * If initVal is provided then
     * - if there is 1 init val, it specify the ceiling of the random integer
     * - if there are 2 values, the first is the bottom of the random val and the second is the ceiling of the val
     */
    public static class RandomFloat extends RandomDouble {
        @Override
        protected List<String> aliases() {
            return C.list("randFloat", "randFlt", "randomFlt");
        }
    }

    /**
     * Generate random `true`, `false`
     */
    public static class RandomBoolean extends Func {
        @Override
        public Object apply() {
            return $.random(true, false);
        }

        @Override
        protected List<String> aliases() {
            return C.list("randBoolean", "randomBool", "randBool");
        }
    }

    /**
     * Generate random long value.
     *
     * If initVal is provided then
     * - if there is 1 init val, it specify the ceiling of the random long value
     * - if there are 2 values, the first is the bottom of the random val and the second is the ceiling of the val
     */
    public static class RandomLong extends Func {
        @Override
        public Object apply() {
            long max = 0;
            long min = 0;
            boolean positive = true;
            if (null != initVal) {
                Object ceilling = initVal;
                if (initVal instanceof List) {
                    List list = (List) initVal;
                    Object bottom = list.get(0);
                    min = $.convert(bottom).toLong();
                    ceilling = list.get(1);
                }
                try {
                    max = $.convert(ceilling).toLong();
                    if (max < 0) {
                        positive = false;
                        if (max > min) {
                            long tmp = min;
                            min = max;
                            max = tmp;
                        }
                        max = min - max;
                    } else {
                        if (max < min) {
                            long tmp = min;
                            min = max;
                            max = tmp;
                        }
                        max = max - min;
                    }
                } catch (Exception e) {
                    warn(e, "RandomLong func init value (max) shall be evaluated to an long, found: " + initVal);
                }
            }
            if (max == 0) {
                max = 100000L;
            }
            long retVal = N.randLong(max);
            if (!positive) {
                retVal = -retVal;
            }
            retVal += min;
            return retVal;
        }

        @Override
        protected List<String> aliases() {
            return C.list("randLong");
        }
    }

    public static void registerTypeConverters() {
        TypeConverterRegistry.INSTANCE.register(new FromLinkedHashMap(Func.class));
        TypeConverterRegistry.INSTANCE.register(new FromString(Func.class));
    }

}
