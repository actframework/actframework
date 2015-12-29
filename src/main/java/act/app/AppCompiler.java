package act.app;

import act.conf.AppConfig;
import act.util.DestroyableBase;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.*;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Locale;
import java.util.Map;

import static org.eclipse.jdt.internal.compiler.impl.CompilerOptions.*;

/**
 * Compile App srccode code in memory. Only used when Act is running
 * in DEV mode
 */
class AppCompiler extends DestroyableBase {

    static final Logger logger = L.get(AppCompiler.class);

    Map<String, Boolean> packagesCache = C.newMap();
    private DevModeClassLoader classLoader;
    private App app;
    private AppConfig conf;
    private CompilerOptions compilerOptions;

    AppCompiler(DevModeClassLoader classLoader) {
        this.classLoader = classLoader;
        this.app = classLoader.app();
        this.conf = app.config();
        configureCompilerOptions();
    }

    @Override
    protected void releaseResources() {
        packagesCache.clear();
        super.releaseResources();
    }

    private void configureCompilerOptions() {
        Map<String, String> map = C.newMap();
        opt(map, OPTION_ReportMissingSerialVersion, IGNORE);
        opt(map, OPTION_LineNumberAttribute, GENERATE);
        opt(map, OPTION_SourceFileAttribute, GENERATE);
        opt(map, OPTION_LocalVariableAttribute, GENERATE);
        opt(map, OPTION_PreserveUnusedLocal, PRESERVE);
        opt(map, OPTION_ReportDeprecation, IGNORE);
        opt(map, OPTION_ReportUnusedImport, IGNORE);
        opt(map, OPTION_Encoding, "UTF-8");
        opt(map, OPTION_Process_Annotations, ENABLED);
        opt(map, OPTION_Source, conf.sourceVersion());
        opt(map, OPTION_TargetPlatform, conf.targetVersion());
        //opt(map, OPTION_TargetPlatform, conf.sourceVersion());
        //opt(map, OPTION_Compliance, conf.sourceVersion());
        compilerOptions = new CompilerOptions(map);
    }

    private void opt(Map map, String key, String val) {
        map.put(key, val);
    }

    public void compile(String... classNames) {
        ICompilationUnit[] compilationUnits = new ICompilationUnit[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            compilationUnits[i] = classLoader.source(classNames[i]).compilationUnit();
        }
        IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.exitOnFirstError();
        IProblemFactory problemFactory = new DefaultProblemFactory(Locale.ENGLISH);

        org.eclipse.jdt.internal.compiler.Compiler jdtCompiler = new Compiler(
                nameEnv, policy, compilerOptions, requestor, problemFactory) {
            @Override
            protected void handleInternalException(Throwable e, CompilationUnitDeclaration ud, CompilationResult result) {
            }
        };

        jdtCompiler.compile(compilationUnits);
    }

    private INameEnvironment nameEnv = new INameEnvironment() {
        @Override
        public NameEnvironmentAnswer findType(char[][] chars) {
            final StringBuilder result = new StringBuilder();
            for (int i = 0; i < chars.length; i++) {
                if (i != 0) {
                    result.append('.');
                }
                result.append(chars[i]);
            }
            return findType(result.toString());
        }

        @Override
        public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
            final StringBuilder result = new StringBuilder();
            for (int i = 0; i < packageName.length; i++) {
                result.append(packageName[i]);
                result.append('.');
            }
            result.append(typeName);
            return findType(result.toString());
        }

        @Override
        public boolean isPackage(char[][] parentPackageName, char[] packageName) {
            StringBuilder sb = new StringBuilder();
            if (parentPackageName != null) {
                for (char[] p : parentPackageName) {
                    sb.append(new String(p));
                    sb.append(".");
                }
            }
            sb.append(new String(packageName));
            String name = sb.toString();
            if (packagesCache.containsKey(name)) {
                return packagesCache.get(name);
            }
            if (classLoader.source(name) != null) {
                packagesCache.put(name, false);
                return false;
            }
            // Check if there is a .java or .class for this resource
            if (classLoader.bytecode(name) != null) {
                packagesCache.put(name, false);
                return false;
            }
            packagesCache.put(name, true);
            return true;
        }

        @Override
        public void cleanup() {

        }

        private NameEnvironmentAnswer findType(String type) {
            try {
                byte[] bytes = classLoader.enhancedBytecode(type);
                if (bytes != null) {
                    ClassFileReader classFileReader = new ClassFileReader(bytes, type.toCharArray(), true);
                    return new NameEnvironmentAnswer(classFileReader, null);
                } else {
                    if (type.startsWith("org.osgl") || type.startsWith("java.") || type.startsWith("javax.")) {
                        return null;
                    }
                }
                Source source = classLoader.source(type);
                if (null == source) {
                    return null;
                } else {
                    return new NameEnvironmentAnswer(source.compilationUnit(), null);
                }
            } catch (ClassFormatException e) {
                throw E.unexpected(e);
            }
        }
    };

    private ICompilerRequestor requestor = new ICompilerRequestor() {
        @Override
        public void acceptResult(CompilationResult result) {
            // If error
            if (result.hasErrors()) {
                for (IProblem problem : result.getErrors()) {
                    char[][] caa = result.packageName;
                    StringBuilder sb = S.builder();
                    for (char[] ca: caa) {
                        sb.append(ca).append(".");
                    }
                    String className = sb.append(new String(problem.getOriginatingFileName())).toString();
                    className = className.substring(0, className.length() - 5);
                    String message = problem.getMessage();
                    if (problem.getID() == IProblem.CannotImportPackage) {
                        // Non sense !
                        message = problem.getArguments()[0] + " cannot be resolved";
                    }
                    throw new CompilationException(classLoader.source(className).file(), message, problem.getSourceLineNumber(), problem.getSourceStart(), problem.getSourceEnd());
                }
            }
            // Something has been compiled
            ClassFile[] clazzFiles = result.getClassFiles();
            for (int i = 0; i < clazzFiles.length; i++) {
                final ClassFile clazzFile = clazzFiles[i];
                final char[][] compoundName = clazzFile.getCompoundName();
                final StringBuffer clazzName = new StringBuffer();
                for (int j = 0; j < compoundName.length; j++) {
                    if (j != 0) {
                        clazzName.append('.');
                    }
                    clazzName.append(compoundName[j]);
                }
                String name = clazzName.toString();
                String name0 = name;
                if (name.contains("$")) {
                    name0 = S.beforeFirst(name, "$");
                }
                Source source = classLoader.source(name0);
                if (null == source) {
                    $.nil();
                    source = classLoader.source(name0);
                }
                if (name != name0) {
                    String innerName = S.afterFirst(name, "$");
                    source.compiled(innerName, clazzFile.getBytes());
                } else {
                    source.compiled(clazzFile.getBytes());
                }
            }
        }
    };

}
