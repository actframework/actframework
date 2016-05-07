package act.dockit;

/**
 * Defines a general contact needs to be implemented by a
 * real document source processor
 */
public interface DocSourceProcessor {
    /**
     * Process the content and return the process result.
     *
     * In general it shall parse and process the raw source e.g. a markdown
     * and return HTML formatted document
     *
     * @param source the document source code
     * @return the processed result
     */
    String process(String source);
}
