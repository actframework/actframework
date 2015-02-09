package org.osgl.oms.app;

import org.osgl._;
import org.osgl.oms.util.Names;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.IO;

import java.io.File;

/**
 * Encapsulate java source unit data including source code, byte code etc.
 * A java source unit specifies a java class
 */
public class Source {
    public static enum State {
        /**
         * Source code loaded
         */
        LOADED,

        /**
         * Byte code compiled out of the source code
         */
        COMPILED,

        /**
         * Tried to compile but there is compile error
         */
        ERROR_COMPILE,

        /**
         * Byte code enhanced by framework
         */
        ENHANCED
    }

    // the source file
    private File file;

    // the class name. Not can't be 1-1 map to file as
    // embedded classes do not have separate source file
    private String className;

    // The source code
    private String code;

    // The byte code
    private byte[] bytes;

    private State state;

    private long ts;

    private Source(File file, String className) {
        E.NPE(file, className);
        this.file = file;
        this.className = className;
    }

    public String className() {
        return className;
    }

    public String code() {
        if (null == code) {
            load();
        }
        return code;
    }

    public byte[] bytes() {
        E.illegalStateIf(null == bytes, "Source code not compiled yet");
        return bytes;
    }

    public void load() {
        code = IO.readContentAsString(file);
        updateState(State.LOADED);
    }

    private void updateState(State state) {
        this.state = state;
        this.ts = _.ms();
    }

    public static Source ofFile(File sourceRoot, File file) {
        return new Source(file, Util.className(sourceRoot, file));
    }

    public static Source ofClass(File sourceRoot, String className) {
        File file = Util.sourceFile(sourceRoot, className);
        if (file.exists() && file.canRead()) {
            return new Source(file, className);
        }
        return null;
    }

    public static enum Util {
        ;
        public static String className(File sourceRoot, File file) {
            return Names.fileToClass(sourceRoot, file.getAbsolutePath());
        }

        public static File sourceFile(File sourceRoot, String className) {
            FastStr s = FastStr.of(className).beforeFirst('$');
            s = s.replace('.', File.separatorChar).append(".java");
            return new File(sourceRoot, s.toString());
        }

        public static void main(String[] args) throws Exception {
        }
    }
}
