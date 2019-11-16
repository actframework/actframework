package act.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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
import org.osgl.util.N;
import org.osgl.util.S;
import osgl.ut.TestBase;

public class DataTableTest extends TestBase {

    public static class Foo {
        public String name;
        public int count;

        public Foo(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    private static Foo randomFoo() {
        String name = S.random();
        int count = N.randInt();
        return new Foo(name, count);
    }

    @Test
    public void testPojoDataTable() {
        Foo foo = randomFoo();
        DataTable fooTable = new DataTable(foo);
        eq(2, fooTable.colCount());
        eq(1, fooTable.rowCount());
        Object row = fooTable.iterator().next();
        eq(foo.name, fooTable.val(row, 1));
        eq(foo.count, fooTable.val(row, 0));
        eq(foo.name, fooTable.val(row, "name"));
        eq(foo.count, fooTable.val(row, "count"));

        DataTable oofTable = fooTable.transpose();
        eq(2, oofTable.colCount());
        eq(2, oofTable.rowCount());
        row = oofTable.iterator().next();
        eq("count", oofTable.val(row, 0));
        eq(foo.count, oofTable.val(row, "_r0"));
    }

}
