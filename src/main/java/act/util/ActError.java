package act.util;

import act.app.SourceInfo;

import java.util.List;

public interface ActError {
    Throwable getCause();
    SourceInfo sourceInfo();
    List<String> stackTrace();
}
