package act.db;

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

import static act.db.util.CriteriaUtil.*;

import act.db.util.CriteriaTestBase;
import org.junit.Test;

public class CriteriaUtilTest extends CriteriaTestBase {
    @Test
    public void test() {
        eq(equal("firstName", "Jack"), c("firstName").eq("Jack")) ;
        eq(notEqual("firstName", "Jack"), c("firstName").ne("Jack"));

        CriteriaGroup group = and(c("age").between(24, 34), or("firstName like, lastName like", "Jen", "Jen"));
        System.out.println(group);
        CriteriaGroup group2 = group.negate();
        System.out.println(group2);
    }

    @Test
    public void parseWithoutOperator() {
        System.out.println(and("firstName, lastName", "Tom", "Smith"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseWithWrongParamNumber() {
        and("firstName, age between", "Thomas", 12);
    }
}
