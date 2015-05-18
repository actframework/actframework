package org.osgl.oms.app;

import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.osgl._;
import org.osgl.oms.util.ClassNames;
import org.osgl.util.E;
import org.osgl.util.FastStr;
import org.osgl.util.IO;
import org.osgl.util.S;

import java.io.File;
import java.util.StringTokenizer;

/**
 * Encapsulate java srccode unit data including srccode code, byte code etc.
 * A java srccode unit specifies a java class
 */
public class Source {
    public static enum State {
        /**
         * The Source instance has been created
         */
        CREATED,

        /**
         * Source code loaded
         */
        LOADED,

        /**
         * Byte code compiled out of the srccode code
         */
        COMPILED,

        /**
         * Tried to compile but there is compile error
         */
        ERROR_COMPILE,

        /**
         * File deleted
         */
        DELETED,

        /**
         * Byte code enhanced by framework
         */
        ENHANCED
    }

    // the srccode file
    private File file;

    // the class className. Can't be 1-1 map to file as
    // embedded classes do not have separate srccode file
    private String simpleName;

    private String packageName;

    // The srccode code
    private String code;

    // The byte code
    private byte[] bytes;

    private State state = State.CREATED;

    private ICompilationUnit compilationUnit;

    private boolean isController;

    private long ts;

    private Source(File file, String className) {
        E.NPE(file, className);
        this.file = file;
        this.simpleName = S.afterLast(className, ".");
        this.packageName = S.beforeLast(className, ".");
        compilationUnit = _compilationUnit();
    }

    public String simpleName() {
        return simpleName;
    }

    public String packageName() {
        return packageName;
    }

    public String className() {
        StringBuilder sb = S.builder(packageName).append(".").append(simpleName);
        return sb.toString();
    }

    public String code() {
        if (null == code) {
            load();
        }
        return code;
    }

    public byte[] bytes() {
        return bytes;
    }

    public File file() {
        return file;
    }

    public void load() {
        code = IO.readContentAsString(file);
        updateState(State.LOADED);
    }

    public void markAsController() {
        isController = true;
    }

    public boolean isController() {
        return isController;
    }

    void compiled(byte[] bytecode) {
        this.bytes = _.notNull(bytecode);
        updateState(State.COMPILED);
    }

    void enhanced(byte[] bytecode) {
        this.bytes = _.notNull(bytecode);
        updateState(State.ENHANCED);
    }

    public void refresh() {
        bytes = null;
        ts = 0L;
        tryLoadSourceFile();
    }

    private void updateState(State state) {
        this.state = state;
        this.ts = _.ms();
    }

    private void tryLoadSourceFile() {
        if (file.exists()) {
            code = IO.readContentAsString(file);
            updateState(State.LOADED);
        } else {
            updateState(State.DELETED);
        }
    }

    public static Source ofFile(File sourceRoot, File file) {
        String className = Util.className(sourceRoot, file);
        return null == className ? null : new Source(file, className);
    }

    public static Source ofClass(File sourceRoot, String className) {
        File file = Util.sourceFile(sourceRoot, className);
        if (file.exists() && file.canRead()) {
            return new Source(file, className);
        }
        return null;
    }

    private ICompilationUnit _compilationUnit() {
        return new ICompilationUnit() {

            char[] mainTypeName = _mainTypeName();
            char[][] packageName = _packageName();
            char[] fileName = _fileName();

            @Override
            public char[] getContents() {
                return code().toCharArray();
            }

            @Override
            public char[] getMainTypeName() {
                return mainTypeName;
            }

            private char[] _mainTypeName() {
                String s = simpleName();
                int pos = s.indexOf('$');
                if (pos > -1) {
                    s = s.substring(0, pos);
                }
                return s.toCharArray();
            }

            @Override
            public char[][] getPackageName() {
                return packageName;
            }

            char[][] _packageName() {
                StringTokenizer tokens = new StringTokenizer(packageName(), ".");
                char[][] ca = new char[tokens.countTokens()][];
                for (int i = 0; i < ca.length; i++) {
                    ca[i] = tokens.nextToken().toCharArray();
                }
                return ca;
            }

            ;

            @Override
            public boolean ignoreOptionalProblems() {
                return false;
            }

            @Override
            public char[] getFileName() {
                return fileName;
            }

            char[] _fileName() {
                String s = simpleName();
                int pos = s.indexOf('$');
                if (pos > -1) {
                    s = s.substring(0, pos);
                }
                s = s.replace('.', '/');
                s = s + ".java";
                return s.toCharArray();
            }
        };
    }

    ICompilationUnit compilationUnit() {
        return compilationUnit;
    }

    public static enum Util {
        ;

        public static String className(File sourceRoot, File file) {
            return ClassNames.sourceFileNameToClassName(sourceRoot, file.getAbsolutePath());
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
