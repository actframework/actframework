package act.metric;

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

import act.ActTestBase;
import org.junit.Before;
import org.junit.Test;

public class SimpleMetricStoreTest extends ActTestBase {
    private SimpleMetricStore store;

    @Before
    public void prepare() {
        store = new SimpleMetricStore(new SimpleMetricPlugin());
    }

    @Test
    public void countOnceShallIncreaseCounterByOne() {
        store.countOnce("abc");
        eq(1L, store.count("abc"));
        store.countOnce("abc");
        eq(2L, store.count("abc"));
    }

    @Test
    public void countOnceShallAggregateToParentCounter() {
        store.countOnce("a:b:c");
        store.countOnce("a:x:y");
        store.countOnce("a:b:d");
        store.countOnce("a:x:z");
        store.countOnce("abc");
        eq(1L, store.count("a:b:c"));
        eq(2L, store.count("a:b"));
        eq(1L, store.count("a:x:z"));
        eq(2L, store.count("a:x"));
        eq(4L, store.count("a"));
    }
}
