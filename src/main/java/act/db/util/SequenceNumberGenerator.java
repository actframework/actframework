package act.db.util;

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

import act.Act;
import act.app.App;
import act.app.DbServiceManager;
import act.cli.Command;
import act.cli.Required;
import act.conf.AppConfig;
import org.osgl.$;

import javax.inject.Inject;

/**
 * Sequence number manipulation utility class
 */
@SuppressWarnings("unused")
public class SequenceNumberGenerator {

    private static volatile  _SequenceNumberGenerator impl;

    public static void registerImpl(_SequenceNumberGenerator impl) {
        SequenceNumberGenerator.impl = $.requireNotNull(impl);
    }

    public static long next(String name) {
        return impl.next(name);
    }

    public static long get(String name) {
        return impl.get(name);
    }


    public static class Provider implements javax.inject.Provider<_SequenceNumberGenerator> {

        @Inject
        private java.util.List<_SequenceNumberGenerator> generators;

        @Override
        public _SequenceNumberGenerator get() {
            if (generators.size() > 1) {
                App app = Act.app();
                AppConfig config = app.config();
                DbServiceManager dbServiceManager = app.dbServiceManager();
                for (_SequenceNumberGenerator gen: generators) {
                    if (!_SequenceNumberGenerator.InMemorySequenceNumberGenerator.class.isInstance(gen)) {
                        try {
                            gen.configure(config, dbServiceManager);
                        } catch (Exception e) {
                            continue;
                        }
                        return gen;
                    }
                }
            }
            return generators.get(0);
        }
    }

    @SuppressWarnings("unused")
    public static class SequenceAdmin {

        @Command(name = "act.seq.next", help = "display the next number in the sequence specified")
        public long generateNext(
                @Required("specify sequence name") String sequence
        ) {
            return next(sequence);
        }

        @Command(name = "act.seq.get", help = "display the current number in the sequence specified")
        public long getCurrent(
                @Required("Specify sequence name") String sequence
        ) {
            return get(sequence);
        }

    }

}
