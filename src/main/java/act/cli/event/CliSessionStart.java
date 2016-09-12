package act.cli.event;

import act.cli.CliSession;
import act.event.ActEvent;

/**
 * This event is emitted synchronously when a {@link CliSession} begin to run
 */
public class CliSessionStart extends ActEvent<CliSession> {
    public CliSessionStart(CliSession source) {
        super(source);
    }
}
