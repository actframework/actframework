package act.inject.util;

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

import act.annotations.Order;
import org.osgl.inject.PostConstructProcessor;

import java.lang.annotation.Annotation;
import java.util.*;

public class Sorter implements PostConstructProcessor<Object> {

    private static final Comparator comp = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            Class<?> c1 = o1.getClass();
            Class<?> c2 = o2.getClass();
            Order order1 = c1.getAnnotation(Order.class);
            Order order2 = c2.getAnnotation(Order.class);
            int p1 = null == order1 ? Order.HIGHEST_PRECEDENCE : order1.value();
            int p2 = null == order2 ? Order.HIGHEST_PRECEDENCE : order2.value();
            int delta = Integer.compare(p1, p2);
            if (0 != delta) {
                return delta;
            }
            return c1.toString().compareTo(c2.toString());
        }
    };


    @Override
    public void process(Object bean, Annotation annotation) {
        if (bean instanceof List) {
            sort((List) bean);
        }
    }

    private void sort(List<?> list) {
        Collections.sort(list, comp);
    }

}
