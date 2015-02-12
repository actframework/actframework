package org.osgl.oms.app;

import java.util.List;

/**
 * Implemented by exceptions with source attachment
 */
public interface SourceInfo {
    String fileName();
    List<String> lines();
    Integer lineNumber();
    boolean isSourceAvailable();
}
