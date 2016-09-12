package act.cli.event;

import act.cli.CliSession;
import act.event.ActEvent;

/**
 * This event is emitted synchronously when a {@link CliSession} terminated
 */
public class CliSessionTerminate extends ActEvent<CliSession> {
    public CliSessionTerminate(CliSession source) {
        super(source);
    }
}
