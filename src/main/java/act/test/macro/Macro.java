package act.test.macro;

/*-
 * #%L
 * ACT Framework
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
import act.test.Scenario;
import act.test.TestSession;
import act.test.util.NamedLogic;
import org.osgl.$;
import org.osgl.util.*;
import org.osgl.util.converter.TypeConverterRegistry;

import java.io.File;
import java.net.URL;
import java.util.List;

public abstract class Macro extends NamedLogic {

    public abstract void run(TestSession session);

    @Override
    protected final Class<? extends NamedLogic> type() {
        return Macro.class;
    }

    public static class ClearFixture extends Macro {

        @Override
        public void run(TestSession session) {
            session.clearFixtures();
        }

        @Override
        protected List<String> aliases() {
            return C.listOf("clear-data");
        }
    }

    public static class ClearSession extends Macro {
        @Override
        public void run(TestSession session) {
            session.clearSession();
        }
    }

    /**
     * Read file content into a string and cache the string
     * with the underscore style of the file name.
     */
    public static class ReadContent extends Macro {

        private String resourcePath;

        @Override
        public void init(Object param) {
            String s = S.string(param);
            if (s.startsWith("/")) {
                s = s.substring(1);
            }
            resourcePath = s;
            E.unexpectedIf(null == tryRead(), "ReadContent init param[%s] must be a valid resource path.", s);
        }

        @Override
        public void run(TestSession session) {
            session.cache(resourcePath, tryRead());
        }

        private String tryRead() {
            File file = new File(resourcePath);
            String content;
            if (file.exists() && file.canRead()) {
                content = IO.read(file).toString();
            } else {
                URL url = Act.getResource(resourcePath);
                content = null == url ? null : IO.read(url).toString();
            }
            return content;
        }

        @Override
        protected List<String> aliases() {
            return C.list("readFile", "readResource", "readFrom");
        }
    }

    public static class Pause extends Macro {

        long time;

        @Override
        public void init(Object param) {
            time = $.convert(param).toLong();
            E.illegalArgumentIf(time < 1);
        }

        @Override
        public void run(TestSession session) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw E.unexpected(e);
            }
        }

        @Override
        protected List<String> aliases() {
            return C.list("sleep");
        }
    }

    public static void registerTypeConverters() {
        TypeConverterRegistry.INSTANCE.register(new FromLinkedHashMap(Macro.class));
        TypeConverterRegistry.INSTANCE.register(new FromString(Macro.class));
    }

    public static void registerActions() {
        new ClearFixture().register();
        new ClearSession().register();
        new ReadContent().register();
        new Pause().register();
    }
}
