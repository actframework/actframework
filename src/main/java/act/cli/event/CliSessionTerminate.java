package act.cli.event;

import act.cli.CliSession;
import act.event.ActEvent;
import act.event.SystemEvent;

/**
 * This event is emitted synchronously when a {@link CliSession} terminated
 */
public class CliSessionTerminate extends ActEvent<CliSession> implements SystemEvent {
    public CliSessionTerminate(CliSession source) {
        super(source);
    }
}
