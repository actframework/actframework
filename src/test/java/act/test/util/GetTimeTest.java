package act.test.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.osgl.util.C;
import osgl.ut.TestBase;

public class GetTimeTest extends TestBase {

    Func.GetTime func;
    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    public void testDateTimeAsOneParam() {
        init("1986-12-11");
        verifyDate("1986-12-11");

        init("1/Nov/2012");
        verifyDate("2012-11-1");

        init("1985-02-02 03:22:11");
        verifyTime("1985-02-02 03:22:11");

        init("19940723 12:33:10");
        verifyTime("1994-07-23 12:33:10");
    }

    @Test
    public void testDeltaAsOneParam() {
        init("30s");
        verifyDelta(30);

        init("1min");
        verifyDelta(60);

        init("-3s");
        verifyDelta(-3);

        init("+1h");
        verifyDelta(60 * 60);
    }

    @Test
    public void testDeltaBeforeDate() {
        init("30s", "1986-01-01 11:10:00");
        DateTime dt = fmt.parseDateTime("1986-01-01 11:10:00");
        eq(dt.plusSeconds(30), get());
    }

    @Test
    public void testDetalAfterDate() {
        init("1986-01-01 11:10:00", "30s");
        DateTime dt = fmt.parseDateTime("1986-01-01 11:10:00");
        eq(dt.plusSeconds(30), get());
    }

    private void init(String ... args) {
        func = new Func.GetTime();
        func.init(C.listOf(args));
    }

    private DateTime get() {
        return (DateTime) func.apply();
    }

    private void verifyDate(String dateTime) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        DateTime dt = fmt.parseDateTime(dateTime);
        eq(dt, get());
    }

    private void verifyTime(String dateTime) {
        DateTime dt = fmt.parseDateTime(dateTime);
        yes(1000 > (dt.getMillis() - get().getMillis()));
    }

    private void verifyDelta(int seconds) {
        DateTime now = DateTime.now().plusSeconds(seconds);
        yes((now.getMillis() - get().getMillis()) < 1000);
    }

}
