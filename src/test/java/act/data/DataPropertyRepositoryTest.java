package act.data;

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
import org.osgl.util.S;
import testapp.model.Person;

import java.util.List;

public class DataPropertyRepositoryTest extends ActTestBase {
    private DataPropertyRepository repo;

    @Before
    public void prepare() throws Exception {
        super.setup();
        repo = new DataPropertyRepository(mockApp);
    }

    @Test
    public void test() {
        List<S.Pair> ls = repo.propertyListOf(Person.class);
        yes(containsField(ls, "firstName"));
        yes(containsLabel(ls, "First name"));
        yes(containsField(ls, "lastName"));
        no(containsLabel(ls, "Last name"));
        yes(containsField(ls, "age"));
        yes(containsField(ls, "address.streetNo"));
        yes(containsField(ls, "address.streetName"));
        yes(containsField(ls, "address.city"));
    }

    private boolean containsLabel(List<S.Pair> pairs, String field) {
        for (S.Pair pair : pairs) {
            if (S.eq(pair._2, field)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsField(List<S.Pair> pairs, String field) {
        for (S.Pair pair : pairs) {
            if (S.eq(pair._1, field)) {
                return true;
            }
        }
        return false;
    }

}
