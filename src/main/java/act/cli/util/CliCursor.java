package act.cli.util;

import act.app.CliContext;

public interface CliCursor {
    void output(CliContext context);
    int records();
    boolean hasNext();
}
