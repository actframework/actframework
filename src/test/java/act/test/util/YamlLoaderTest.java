package act.test.util;

/*-
 * #%L
 * ACT E2E Plugin
 * %%
 * Copyright (C) 2018 ActFramework
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

import act.Act;
import act.app.DaoLocator;
import act.db.Dao;
import act.test.model.User;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

public class YamlLoaderTest extends TestTestBase {

    YamlLoader loader;
    DaoLocator daoLocator;

    @Before
    public void prepareLoader() {
        Act.registerTypeConverters();
        loader = new YamlLoader("act.test.model");
    }

    @Before
    public void prepareDaoLocator() {
        daoLocator = new DaoLocator() {
            @Override
            public Dao dao(Class<?> aClass) {
                return null;
            }
        };
    }

    @Test
    public void testLoad() {
        Map<String, Object> data = loader.loadFixture("init-data.yml", daoLocator);
        eq(5, data.size());
        User user = (User) data.get("green");
        notNull(user);
        eq("Green Luo", user.name);
        eq(1, user.courses.size());
        eq("Maths", user.courses.get(0).name);
        assertDate(user.birthday, 1919, 1, 1);

        user = (User) data.get("black");
        notNull(user);
        eq("Black Smith", user.name);
        eq(2, user.courses.size());
        eq("Maths", user.courses.get(0).name);
        eq("History", user.courses.get(1).name);
        assertDate(user.birthday, 1818, 2, 2);

        user = (User) data.get("john");
        notNull(user);
        eq("John Brad", user.name);
        eq(3, user.id);
        isNull(user.birthday);
        eq(2, user.courses.size());
        eq("Physics", user.courses.get(0).name);
        eq("Science", user.courses.get(1).name);
        eq(4, user.courses.get(1).id);
    }

    private void assertDate(DateTime dt, int y, int m, int d) {
        eq(y, dt.getYear());
        eq(m, dt.getMonthOfYear());
        eq(d, dt.getDayOfMonth());
    }

}
