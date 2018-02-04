package testapp.metric;

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

import act.metric.MeasureTime;

public class TestBed {

    @MeasureTime("pi")
    public void foo(int len) {
        pi(len);
    }

    public static double pi(int len) {
        double sum = 0d;
        for (double i = 0; i < len; i++) {
            if (i % 2 == 0) // if the remainder of `i/2` is 0
                sum += -1 / (2 * i - 1);
            else
                sum += 1 / (2 * i - 1);
        }
        return sum;
    }


}
