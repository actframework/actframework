package act.app;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.util.ClassNames;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.osgl.$;
import org.osgl.util.*;

import java.io.File;
import java.util.*;

/**
 * Encapsulate java srccode unit data including srccode code, byte code etc.
 * A java srccode unit specifies a java class
 */
public class Source {
    public enum State {
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

    private String className;

    // The srccode code
    private String code;

    // The byte code
    private byte[] bytes;

    private Map<String, byte[]> innerBytes = new HashMap<>();

    private State state = State.CREATED;

    private ICompilationUnit compilationUnit;

    private boolean isController;

    private long ts;

    private Source(File file, String className) {
        E.NPE(file, className);
        this.file = file;
        this.simpleName = S.afterLast(className, ".");
        this.packageName = S.beforeLast(className, ".");
        this.className = className;
        compilationUnit = _compilationUnit();
    }

    public String simpleName() {
        return simpleName;
    }

    public String packageName() {
        return packageName;
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

    public List<String> lines() {
        return C.listOf(code.split("\n"));
    }

    public byte[] bytes() {
        return bytes;
    }

    public byte[] bytes(String innerClass) {
        return innerBytes.get(innerClass);
    }

    public Set<String> innerClassNames() {
        return innerBytes.keySet();
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
        this.bytes = $.requireNotNull(bytecode);
        updateState(State.COMPILED);
    }

    void compiled(String innerClassName, byte[] bytecode) {
        innerBytes.put(innerClassName, bytecode);
    }

    void enhanced(byte[] bytecode) {
        this.bytes = $.requireNotNull(bytecode);
        updateState(State.ENHANCED);
    }

    public void refresh() {
        bytes = null;
        ts = 0L;
        tryLoadSourceFile();
    }

    private void updateState(State state) {
        this.state = state;
        this.ts = $.ms();
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

    public static Source ofClass(List<File> sourceRoots, String className) {
        File file = Util.sourceFile(sourceRoots, className);
        if (null != file) {
            return new Source(file, className);
        }
        return null;
    }

    public static Source ofInnerClass(File sourceFile, String innerClassName) {
        return new Source(sourceFile, innerClassName);
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

    public enum Util {
        ;

        public static String className(File sourceRoot, File file) {
            return ClassNames.sourceFileNameToClassName(sourceRoot, file.getAbsolutePath());
        }

        public static File sourceFile(List<File> sourceRoots, String className) {
            FastStr s = FastStr.of(className).beforeFirst('$');
            s = s.replace('.', File.separatorChar).append(".java");
            for (File sourceRoot : sourceRoots) {
                File file = new File(sourceRoot, s.toString());
                if (file.canRead()) {
                    return file;
                }
            }
            return null;
        }

        public static void main(String[] args) throws Exception {
        }
    }
}
