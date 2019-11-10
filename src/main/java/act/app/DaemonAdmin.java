package act.app;

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

import act.cli.*;
import act.data.JodaDateTimeCodec;
import act.util.PropertySpec;
import org.joda.time.DateTime;
import org.osgl.$;
import org.osgl.inject.annotation.Provided;
import org.osgl.util.*;

import java.util.Map;

/**
 * Access application daemon status
 */
@SuppressWarnings("unused")
public class DaemonAdmin {

    @Command(name = "act.daemon.list,act.daemon,act.daemons", help = "List app daemons")
    @PropertySpec("id,state")
    public Iterable<Daemon> list(@Optional("specify filter string") final String q) {
        final C.List<Daemon> daemons = C.list(App.instance().registeredDaemons());
        if (S.notBlank(q)) {
            return daemons.filter(new $.Predicate<Daemon>() {
                @Override
                public boolean test(Daemon daemon) {
                    String s = S.string(daemon.id());
                    return s.toLowerCase().contains(q) || s.matches(q);
                }
            });
        }
        return daemons;
    }

    @Command(name = "act.daemon.start", help = "Start app daemon")
    public void start(
            @Required("specify daemon id") String id,
            CliContext context
    ) {
        Daemon daemon = get(id, context);
        daemon.start();
        report(daemon, context);
    }

    @Command(name = "act.daemon.stop", help = "Stop app daemon")
    public void stop(
            @Required("specify daemon id") String id,
            CliContext context
    ) {
        Daemon daemon = get(id, context);
        daemon.stop();
        report(daemon, context);
    }

    @Command(name = "act.daemon.restart", help = "Re-Start app daemon")
    public void restart(
            @Required("specify daemon id") String id,
            CliContext context
    ) {
        Daemon daemon = get(id, context);
        daemon.restart();
        report(daemon, context);
    }

    @Command(name = "act.daemon.status", help = "Report app daemon status")
    public void status(
            @Required("specify daemon id") String id,
            CliContext context,
            @Provided JodaDateTimeCodec fmt
    ) {
        Daemon daemon = get(id, context);
        Daemon.State state = daemon.state();
        DateTime ts = daemon.timestamp();
        Exception lastError = daemon.lastError();
        context.println("Daemon[%s]: %s at %s", id, state, fmt.toString(ts));
        if (null != lastError) {
            DateTime errTs = daemon.errorTimestamp();
            if (null != errTs) {
                context.println("Last error: %s at %s", E.stackTrace(lastError), fmt.toString(errTs));
            } else {
                context.println("Last error: %s", E.stackTrace(lastError));
            }
        }
        Map<String, Object> attributes = daemon.getAttributes();
        if (!attributes.isEmpty()) {
            context.println("Attributes: %s", attributes);
        }
    }

    private static Daemon get(String id, CliContext context) {
        Daemon daemon = App.instance().registeredDaemon(id);
        if (null == daemon) {
            context.println("Unknown daemon: %s", id);
            return null;
        }
        return daemon;
    }

    private static void report(Daemon daemon, CliContext context) {
        context.println("Daemon[%s]: %s", daemon.id(), daemon.state());
    }
}
