package act.app;

import org.osgl.util.E;

import java.util.List;

public class SourceInfoImpl implements SourceInfo {

    private Source source;
    private int line;
    public SourceInfoImpl(Source source, int line) {
        E.NPE(source);
        this.source = source;
        this.line = line;
    }

    @Override
    public String fileName() {
        return source.file().getName();
    }

    @Override
    public List<String> lines() {
        return source.lines();
    }

    @Override
    public Integer lineNumber() {
        return line;
    }

    @Override
    public boolean isSourceAvailable() {
        return true;
    }
}
