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
import act.util.Ordered;
import org.osgl.inject.PostConstructProcessor;

import java.lang.annotation.Annotation;
import java.util.*;

public class Sorter implements PostConstructProcessor<Object> {

    public static final Comparator COMPARATOR = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            if (o1 == null) {
                o1 = -1;
            }
            if (o2 == null) {
                o2 = 1;
            }
            if (o1 == o2 || o1.equals(o2)) {
                return 0;
            }
            int p1 = orderOf(o1);
            int p2 = orderOf(o2);
            int delta = Integer.compare(p1, p2);
            if (0 != delta) {
                return delta;
            }
            Class<?> c1 = o1.getClass();
            Class<?> c2 = o2.getClass();
            if (c1 == c2) {
                if (Comparable.class.isAssignableFrom(c1)) {
                    return ((Comparable) o1).compareTo(o2);
                }
                return o1.toString().compareTo(o2.toString());
            }
            return c1.toString().compareTo(c2.toString());
        }
    };

    public static final Comparator REVERSE_COMPARATOR = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            return COMPARATOR.compare(o2, o1);
        }
    };

    public static final Comparator comparator(boolean reverseOrder) {
        return reverseOrder ? REVERSE_COMPARATOR : COMPARATOR;
    }

    private static int orderOf(Object o) {
        if (o instanceof Ordered) {
            return ((Ordered) o).order();
        }
        Class<?> c = o.getClass();
        Order order = c.getAnnotation(Order.class);
        return null == order ? Order.HIGHEST_PRECEDENCE : order.value();
    }


    @Override
    public void process(Object bean, Annotation annotation) {
        if (bean instanceof List) {
            sort((List) bean);
        }
    }

    public static void sort(List<?> list) {
        Collections.sort(list, COMPARATOR);
    }

}
