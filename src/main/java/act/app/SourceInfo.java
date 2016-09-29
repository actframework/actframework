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

    public static abstract class Base implements SourceInfo {
        protected String fileName;
        protected List<String> lines;
        protected int lineNumber;

        @Override
        public String fileName() {
            return fileName;
        }

        @Override
        public List<String> lines() {
            return lines;
        }

        @Override
        public Integer lineNumber() {
            return lineNumber;
        }

        @Override
        public boolean isSourceAvailable() {
            return true;
        }
    }
}
