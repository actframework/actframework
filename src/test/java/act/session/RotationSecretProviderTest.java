package act.session;

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

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import osgl.ut.TestBase;

import static act.session.RotationSecretProvider.roundToPeriod;

@RunWith(Enclosed.class)
public class RotationSecretProviderTest extends TestBase {

    public static class RoundToPeriodTest extends TestBase {

        @Test
        public void test() {
            v(1, 1);
            v(2, 2);
            v(3, 3);
            v(4, 4);
            v(5, 5);
            v(6, 6);
            v(10, 7);
            v(10, 10);
            v(12, 11);
            v(15, 13);
            v(20, 16);
            v(30, 44);
            v(60, 45);
            v(60, 70);
            v(60, 60);
            v(60, 89);
            v(120, 90);
            v(120, 100);
            v(180, 190);
        }

        private static void v(int expected, int input) {
            eq(expected, roundToPeriod(input));
        }
    }

}
