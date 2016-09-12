package act.cli.util;

import act.cli.CliContext;

public interface CliCursor {
    void output(CliContext context);
    int records();
    boolean hasNext();
}
