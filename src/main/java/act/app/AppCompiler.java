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

import static org.eclipse.jdt.internal.compiler.impl.CompilerOptions.*;

import act.Act;
import act.conf.AppConfig;
import act.metric.*;
import act.metric.Timer;
import act.util.LogSupportedDestroyableBase;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.*;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.*;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.*;

/**
 * Compile App srccode code in memory. Only used when Act is running
 * in DEV mode
 */
class AppCompiler extends LogSupportedDestroyableBase {

    Map<String, Boolean> packagesCache = new HashMap<>();
    private DevModeClassLoader classLoader;
    private App app;
    private AppConfig conf;
    private CompilerOptions compilerOptions;
    private Metric metric;

    AppCompiler(DevModeClassLoader classLoader) {
        this.classLoader = classLoader;
        this.app = classLoader.app();
        this.conf = app.config();
        this.metric = Act.metricPlugin().metric(MetricInfo.CLASS_LOADING);
        configureCompilerOptions();
    }

    @Override
    protected void releaseResources() {
        packagesCache.clear();
        super.releaseResources();
    }

    private void configureCompilerOptions() {
        Map<String, String> map = new HashMap<>();
        map.putAll((Map)System.getProperties());
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
        opt(map, OPTION_Compliance, conf.sourceVersion());
        opt(map, OPTION_MethodParametersAttribute, GENERATE);
        compilerOptions = new CompilerOptions(map);
    }

    private void opt(Map map, String key, String val) {
        map.put(key, val);
    }

    public void compile(Collection<Source> sources) {
        Timer timer = metric.startTimer("act:classload:compile:_all");
        int len = sources.size();
        ICompilationUnit[] compilationUnits = new ICompilationUnit[len];
        int i = 0;
        for (Source source: sources) {
            compilationUnits[i++] = source.compilationUnit();
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
        timer.stop();
    }

    public void compile(String className) {
        Timer timer = metric.startTimer("act:classload:compile:" + className);
        ICompilationUnit[] compilationUnits = new ICompilationUnit[1];
        compilationUnits[0] = classLoader.source(className).compilationUnit();
        IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.exitOnFirstError();
        IProblemFactory problemFactory = new DefaultProblemFactory(Locale.ENGLISH);

        org.eclipse.jdt.internal.compiler.Compiler jdtCompiler = new Compiler(
                nameEnv, policy, compilerOptions, requestor, problemFactory) {
            @Override
            protected void handleInternalException(Throwable e, CompilationUnitDeclaration ud, CompilationResult result) {
            }
        };

        jdtCompiler.compile(compilationUnits);
        timer.stop();
    }

    private INameEnvironment nameEnv = new INameEnvironment() {
        @Override
        public NameEnvironmentAnswer findType(char[][] chars) {
            final S.Buffer result = S.buffer();
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
            final S.Buffer result = S.buffer();
            for (int i = 0; i < packageName.length; i++) {
                result.append(packageName[i]);
                result.append('.');
            }
            result.append(typeName);
            return findType(result.toString());
        }

        @Override
        public boolean isPackage(char[][] parentPackageName, char[] packageName) {
            S.Buffer sb = S.buffer();
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
                byte[] bytes;
                Source source;
                if (Act.isDev()) {
                    source = classLoader.source(type);
                    if (null != source) {
                        return new NameEnvironmentAnswer(source.compilationUnit(), null);
                    }
                }
                bytes = classLoader.enhancedBytecode(type);
                if (bytes != null) {
                    ClassFileReader classFileReader = new ClassFileReader(bytes, type.toCharArray(), true);
                    return new NameEnvironmentAnswer(classFileReader, null);
                } else {
                    if (type.startsWith("org.osgl") || type.startsWith("java.") || type.startsWith("javax.")) {
                        return null;
                    }
                }
                if (Act.isDev()) {
                    return null;
                }
                source = classLoader.source(type);
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
                    if (null == caa) {
                        caa = result.compilationUnit.getPackageName();
                    }
                    S.Buffer sb = S.buffer();
                    if (null != caa) {
                        for (char[] ca : caa) {
                            sb.append(ca).append(".");
                        }
                    }
                    String className = sb.append(new String(problem.getOriginatingFileName())).toString();
                    className = className.substring(0, className.length() - 5);
                    String message = problem.getMessage();
                    if (problem.getID() == IProblem.CannotImportPackage) {
                        // Non sense !
                        message = problem.getArguments()[0] + " cannot be resolved";
                    }
                    Source src = classLoader.source(className);
                    if (null != src) {
                        int column = 0;
                        if (problem instanceof DefaultProblem) {
                            column = ((DefaultProblem) problem).getSourceColumnNumber();
                        }
                        throw new CompilationException(src.file(), message, problem.getSourceLineNumber(), column, problem.getSourceStart(), problem.getSourceEnd());
                    } else {
                        throw new CompilationException(problem.getMessage());
                    }
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
                if (null == source) {
                    throw E.unexpected("Cannot locate source file for %s. \nPlease make sure you do not have non-nested classes defined in source file of other class", name);
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
