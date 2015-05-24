package act.app;

import java.util.List;

/**
 * Implemented by exceptions with srccode attachment
 */
public interface SourceInfo {
    String fileName();

    List<String> lines();

    Integer lineNumber();

    boolean isSourceAvailable();
}
