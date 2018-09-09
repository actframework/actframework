package act.db.util;

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

import act.data.annotation.Data;
import act.util.SimpleBean;

import java.util.List;

@Data
public class Page<MODEL> implements SimpleBean {
    /**
     * The page number.
     */
    public int no;

    /**
     * The page size.
     */
    public int size;

    /**
     * The retrieved data for the page.
     */
    public Iterable<MODEL> list;

    /**
     * The total number of data can be retrieved.
     */
    public long total;

    /**
     * The order specification.
     * Each element in the list shall be a field name, optionally prefixed
     * with `-` symbol, meaning sort by the field in descending order.
     */
    public List<String> orderBy;

    public Page(Iterable<MODEL> list, long total) {
        this.list = list;
        this.total = total;
    }

    public static <T> Page<T> of(Iterable<T> models, long total) {
        return new Page<>(models, total);
    }
}
