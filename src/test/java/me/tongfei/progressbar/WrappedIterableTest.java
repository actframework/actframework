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

import org.osgl.util.C;

import java.util.List;

/**
 * @author Tongfei Chen
 */
public class WrappedIterableTest {

    static public void main(String[] args) throws Exception {
        System.out.println(System.getenv("TERM"));

        List<Integer> sizedColl = C.newList();
        for (int i = 0; i < 10000; ++i) {
            sizedColl.add(i);
        }

        for (Integer x : ProgressBar.wrap(sizedColl, "Traverse")) {
            Thread.sleep(2);
        }
    }

}
