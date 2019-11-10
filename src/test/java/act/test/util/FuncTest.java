package act.test.util;

/*-
 * #%L
 * ACT E2E Plugin
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

import act.test.func.Func;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.osgl.util.C;
import org.osgl.util.S;
import osgl.ut.TestBase;

public class FuncTest extends TestBase {

    @Test
    public void randOf() {
        Func func = new Func.RandomOf();
        func.init(C.list("foo", "bar"));
        Object o = func.apply();
        yes(C.setOf("foo", "bar").contains(o));
    }

    @Test
    public void randStrWithoutInitVal() {
        Func func = new Func.RandomStr();
        Object o = func.apply();
        notNull(o);
        yes(o instanceof String);
        int len = S.string(o).length();
        yes(len >= 5);
        yes(len < 15);
    }

    @Test
    public void randStrWithValidInitVal() {
        Func func = new Func.RandomStr();
        func.init(10);
        Object o = func.apply();
        notNull(o);
        yes(o instanceof String);
        eq(10, S.string(o).length());
    }

    @Test
    public void randIntWithoutInitVal() {
        Func func = new Func.RandomInt();
        Object o = func.apply();
        notNull(o);
        yes(o instanceof Integer);
        int i = (Integer) o;
        yes(i >= 0);
        yes(i < 100);
    }

    @Test
    public void randIntWithSingleInitVal() {
        Func func = new Func.RandomInt();
        func.init(3);
        Object o = func.apply();
        notNull(o);
        yes(o instanceof Integer);
        int i = (Integer) o;
        yes(i >= 0);
        yes(i < 3);
    }

    @Test
    public void randIntWithSingleInitVal2() {
        Func func = new Func.RandomInt();
        func.init(-3);
        Object o = func.apply();
        notNull(o);
        yes(o instanceof Integer);
        int i = (Integer) o;
        yes(i <= 0);
        yes(i > -3);
    }

    @Test
    public void randIntWithTwoInitVals() {
        Func func = new Func.RandomInt();
        func.init(C.list("10", "14"));
        Object o = func.apply();
        notNull(o);
        yes(o instanceof Integer);
        int i = (Integer) o;
        yes(i >= 10);
        yes(i < 14);
    }

    @Test
    public void randIntWithTwoInitVals2() {
        Func func = new Func.RandomInt();
        func.init(C.list("-10", "-14"));
        Object o = func.apply();
        notNull(o);
        yes(o instanceof Integer);
        int i = (Integer) o;
        yes(i <= -10);
        yes(i > -14);
    }


    @Test
    public void randLongWithoutInitVal() {
        Func func = new Func.RandomLong();
        Object o = func.apply();
        notNull(o);
        yes(o instanceof Long);
        long i = (Long) o;
        yes(i >= 0);
        yes(i < 100000L);
    }

    @Test
    public void RandLongWithSingleInitVal() {
        Func func = new Func.RandomLong();
        func.init(3);
        Object o = func.apply();
        notNull(o);
        yes(o instanceof Long);
        long i = (Long) o;
        yes(i >= 0);
        yes(i < 3);
    }

    @Test
    public void RandLongWithSingleInitVal2() {
        Func func = new Func.RandomLong();
        func.init(-3);
        Object o = func.apply();
        notNull(o);
        yes(o instanceof Long);
        long i = (Long) o;
        yes(i <= 0);
        yes(i > -3);
    }

    @Test
    public void RandLongWithTwoInitVals() {
        Func func = new Func.RandomLong();
        func.init(C.list("10", "14"));
        Object o = func.apply();
        notNull(o);
        yes(o instanceof Long);
        long i = (Long) o;
        yes(i >= 10);
        yes(i < 14);
    }

    @Test
    public void RandLongWithTwoInitVals2() {
        Func func = new Func.RandomLong();
        func.init(C.list("-10", "-14"));
        Object o = func.apply();
        notNull(o);
        yes(o instanceof Long);
        long i = (Long) o;
        yes(i <= -10);
        yes(i > -14);
    }

    @Test
    public void testToday() {
        Func.Today func = new Func.Today();
        Object obj = func.apply();
        yes(obj instanceof LocalDate);
        LocalDate funcToday = (LocalDate) obj;
        eq(LocalDate.now(), funcToday);
    }

    @Test
    public void testTomorrow() {
        Func.Tomorrow func = new Func.Tomorrow();
        Object obj = func.apply();
        yes(obj instanceof LocalDate);
        LocalDate funcToday = (LocalDate) obj;
        eq(LocalDate.now().plusDays(1), funcToday);
    }

    @Test
    public void testNow() {
        Func.Now func = new Func.Now();
        Object obj = func.apply();
        yes(obj instanceof DateTime);
        DateTime funcNow = (DateTime) obj;
        yes((DateTime.now().getMillis() - funcNow.getMillis()) < 2000);
    }

    @Test
    public void testNextMinute() {
        Func.NextMinute func = new Func.NextMinute();
        Object obj = func.apply();
        yes(obj instanceof DateTime);
        DateTime funcNow = (DateTime) obj;
        yes((DateTime.now().plusMinutes(1).getMillis() - funcNow.getMillis()) < 2000);
    }


    @Test
    public void testNextHour() {
        Func.NextHour func = new Func.NextHour();
        Object obj = func.apply();
        yes(obj instanceof DateTime);
        DateTime funcNow = (DateTime) obj;
        yes((DateTime.now().plusHours(1).getMillis() - funcNow.getMillis()) < 2000);
    }

}
