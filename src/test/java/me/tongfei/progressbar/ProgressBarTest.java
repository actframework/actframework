package me.tongfei.progressbar;

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

import java.util.ArrayList;

/**
 * @author Tongfei Chen
 */
public class ProgressBarTest {

    static public void main(String[] args) throws Exception {
        ProgressBar pb = new ProgressBar("Test", 5, 50, System.out, ProgressBarStyle.UNICODE_BLOCK).start();

        double x = 1.0;
        double y = x * x;

        ArrayList<Integer> l = new ArrayList<Integer>();

        System.out.println("\n\n\n\n\n");

        for (int i = 0; i < 10000; i++) {
            int sum = 0;
            for (int j = 0; j < i * 2000; j++)
                sum += j;
            l.add(sum);

            pb.step();
            if (pb.getCurrent() > 8000) pb.maxHint(10000);

        }
        pb.stop();
        System.out.println("Hello");
    }

}
