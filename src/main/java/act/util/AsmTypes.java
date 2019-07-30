package act.util;

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

import act.app.ActionContext;
import act.app.App;
import act.asm.Opcodes;
import act.asm.Type;
import act.cli.*;
import act.conf.AppConfig;
import act.data.annotation.ReadContent;
import act.inject.Context;
import act.inject.SessionVariable;
import act.mail.MailerContext;
import act.view.ProvidesImplicitTemplateVariable;
import org.osgl.inject.annotation.Provides;
import org.osgl.mvc.annotation.Bind;
import org.osgl.mvc.annotation.Param;
import org.osgl.mvc.result.Result;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Set;

public enum AsmTypes implements Opcodes {
    ;
    public static AsmType<Void> VOID = new AsmType<>(Void.class);
    public static AsmType<Object> OBJECT = new AsmType<>(Object.class);
    public static AsmType<String> STRING = new AsmType<>(String.class);
    public static AsmType<ActContext> ACT_CONTEXT = new AsmType<>(ActContext.class);
    public static AsmType<ActionContext> ACTION_CONTEXT = new AsmType<>(ActionContext.class);
    public static AsmType<MailerContext> MAILER_CONTEXT = new AsmType<>(MailerContext.class);
    public static AsmType<App> APP = new AsmType<>(App.class);

    public static AsmType<AppConfig> APP_CONFIG = new AsmType<>(AppConfig.class);
    public static AsmType<Param> PARAM = new AsmType<>(Param.class);
    public static AsmType<SessionVariable> CLI_SESSION_ATTRIBUTE = new AsmType<>(SessionVariable.class);
    public static AsmType<Bind> BIND = new AsmType<>(Bind.class);
    public static AsmType<Context> CONTEXT = new AsmType<>(Context.class);
    public static AsmType<Result> RESULT = new AsmType<>(Result.class);

    public static AsmType<PropertySpec> PROPERTY_SPEC = new AsmType<>(PropertySpec.class);

    public static AsmType<Command> COMMAND = new AsmType<>(Command.class);
    public static AsmType<Optional> OPTIONAL = new AsmType<>(Optional.class);
    public static AsmType<ReadContent> READ_FILE_CONTENT = new AsmType<>(ReadContent.class);
    public static AsmType<Required> REQUIRED = new AsmType<>(Required.class);
    public static AsmType<TableView> TABLE_VIEW = new AsmType<>(TableView.class);
    public static AsmType<act.cli.JsonView> JSON_VIEW_DEPRECATED = new AsmType<>(act.cli.JsonView.class);
    public static AsmType<JsonView> JSON_VIEW = new AsmType<>(JsonView.class);
    public static AsmType<TreeView> TREE_VIEW = new AsmType<>(TreeView.class);
    public static AsmType<act.cli.CsvView> CSV_VIEW_DEPRECATED = new AsmType<>(act.cli.CsvView.class);
    public static AsmType<CsvView> CSV_VIEW = new AsmType<>(CsvView.class);
    public static AsmType<CommandPrefix> CMD_PREFIX = new AsmType<>(CommandPrefix.class);

    public static AsmType<SubClassFinder> SUB_CLASS_FINDER = new AsmType<>(SubClassFinder.class);
    public static AsmType<AnnotatedClassFinder> ANN_CLASS_FINDER = new AsmType<>(AnnotatedClassFinder.class);

    public static AsmType<Provides> PROVIDES = new AsmType<>(Provides.class);

    public static AsmType<ProvidesImplicitTemplateVariable> TEMPLATE_VARIABLE = new AsmType<>(ProvidesImplicitTemplateVariable.class);


    public static final Type RETURN_VOID = Type.VOID_TYPE;

    public static final Type OBJECT_TYPE = OBJECT.asmType();
    public static final String OBJECT_NAME = OBJECT.className();
    public static final String OBJECT_INTERNAL_NAME = OBJECT.internalName();
    public static final String OBJECT_DESC = OBJECT.desc();

    public static final Type STRING_TYPE = STRING.asmType();
    public static final String STRING_NAME = STRING.className();
    public static final String STRING_INTERNAL_NAME = STRING.internalName();
    public static final String STRING_DESC = STRING.desc();

    public static final Type APP_TYPE = APP.asmType();
    public static final String APP_NAME = APP.className();
    public static final String APP_INTERNAL_NAME = APP.internalName();
    public static final String APP_DESC = APP.desc();

    public static final Type APP_CONFIG_TYPE = APP_CONFIG.asmType();
    public static final String APP_CONFIG_NAME = APP_CONFIG.className();
    public static final String APP_CONFIG_INTERNAL_NAME = APP_CONFIG.internalName();
    public static final String APP_CONFIG_DESC = APP_CONFIG.desc();

    public static final Type MAILER_CONTEXT_TYPE = MAILER_CONTEXT.asmType();
    public static final String MAILER_CONTEXT_NAME = MAILER_CONTEXT.className();
    public static final String MAILER_CONTEXT_INTERNAL_NAME = MAILER_CONTEXT.internalName();
    public static final String MAILER_CONTEXT_DESC = MAILER_CONTEXT.desc();

    public static final Type ACT_CONTEXT_TYPE = ACT_CONTEXT.asmType();
    public static final String ACT_CONTEXT_NAME = ACT_CONTEXT.className();
    public static final String ACT_CONTEXT_INTERNAL_NAME = ACT_CONTEXT.internalName();
    public static final String ACT_CONTEXT_DESC = ACT_CONTEXT.desc();

    public static final Type ACTION_CONTEXT_TYPE = ACTION_CONTEXT.asmType();
    public static final String ACTION_CONTEXT_NAME = ACTION_CONTEXT.className();
    public static final String ACTION_CONTEXT_INTERNAL_NAME = ACTION_CONTEXT.internalName();
    public static final String ACTION_CONTEXT_DESC = ACTION_CONTEXT.desc();

    public static final Type PARAM_TYPE = PARAM.asmType();
    public static final String PARAM_NAME = PARAM.className();
    public static final String PARAM_INTERNAL_NAME = PARAM.internalName();
    public static final String PARAM_DESC = PARAM.desc();

    public static final Type BIND_TYPE = BIND.asmType();
    public static final String BIND_NAME = BIND.className();
    public static final String BIND_INTERNAL_NAME = BIND.internalName();
    public static final String BIND_DESC = BIND.desc();

    public static final Type RESULT_TYPE = RESULT.asmType();
    public static final String RESULT_NAME = RESULT.className();
    public static final String RESULT_INTERNAL_NAME = RESULT.internalName();
    public static final String RESULT_DESC = RESULT.desc();

    public static String methodDesc(Class retType, Class... paramTypes) {
        S.Buffer sb = S.newBuffer("(");
        for (Class c : paramTypes) {
            Type t = Type.getType(c);
            sb.append(t.getDescriptor());
        }
        sb.append(")");
        if (Void.class.equals(retType)) {
            sb.append(Type.VOID_TYPE.getDescriptor());
        } else {
            sb.append(Type.getType(retType).getDescriptor());
        }
        return sb.toString();
    }

    public static boolean isStatic(int access) {
        return (ACC_STATIC & access) > 0;
    }

    public static boolean isPublic(int access) {
        return (ACC_PUBLIC & access) > 0;
    }

    public static boolean isAbstract(int access) {
        return (ACC_ABSTRACT & access) > 0;
    }

    public static boolean isPublicNotAbstract(int access) {
        return isPublic(access) && !isAbstract(access);
    }

    private static Set<Type> contextTypes = C.set(
            Type.getType(App.class),
            Type.getType(AppConfig.class),
            Type.getType(ActionContext.class),
            Type.getType(MailerContext.class),
            Type.getType(CliContext.class),
            Type.getType(ActContext.class),
            Type.getType(Exception.class),
            Type.getType(Result.class)
    );

    public static boolean isContextType(Type type) {
        return contextTypes.contains(type);
    }
}
