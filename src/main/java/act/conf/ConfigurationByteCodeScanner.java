package act.conf;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import act.app.AppByteCodeScannerBase;
import act.app.event.SysEventId;
import act.asm.AnnotationVisitor;
import act.asm.FieldVisitor;
import act.asm.Type;
import act.inject.DefaultValue;
import act.inject.DependencyInjector;
import act.util.AsmTypes;
import act.util.ByteCodeVisitor;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.annotation.Configuration;
import org.osgl.inject.loader.ConfigurationValueLoader;
import org.osgl.util.C;
import org.osgl.util.Const;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationByteCodeScanner extends AppByteCodeScannerBase {

    private static final String CONF_DESC = Type.getType(Configuration.class).getDescriptor();
    private String className;
    // map field name to configuration key
    private Map<String, String> staticConfigurationFields = new HashMap<>();

    @Override
    protected boolean shouldScan(String className) {
        return true;
    }

    @Override
    protected void reset(String className) {
        super.reset(className);
        this.className = className;
        staticConfigurationFields.clear();
    }

    @Override
    public ByteCodeVisitor byteCodeVisitor() {
        return new ByteCodeVisitor() {
            @Override
            public FieldVisitor visitField(int access, final String name, String desc, String signature, Object value) {
                FieldVisitor fv = super.visitField(access, name, desc, signature, value);
                boolean isStatic = AsmTypes.isStatic(access);
                if (!isStatic) {
                    return fv;
                }
                final String fieldName = name;
                return new FieldVisitor(ASM5, fv) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        AnnotationVisitor av = super.visitAnnotation(desc, visible);
                        if (S.eq(CONF_DESC, desc)) {
                            return new AnnotationVisitor(ASM5, av) {
                                @Override
                                public void visit(String name, Object value) {
                                    staticConfigurationFields.put(fieldName, S.string(value));
                                    super.visit(name, value);
                                }
                            };
                        }
                        return av;
                    }
                };
            }
        };
    }

    @Override
    public void scanFinished(final String className) {
        E.unexpectedIf(S.neq(this.className, className), "oops");
        if (staticConfigurationFields.isEmpty()) {
            return;
        }
        final DependencyInjector injector = app().injector();
        final Map<String, String> staticConfigurationFields = new HashMap<>(this.staticConfigurationFields);
        app().jobManager().on(SysEventId.PRE_START, "ConfigurationByteCodeScanner:loadIntoStaticFieldsOf:" + className, new Runnable() {
            @Override
            public void run() {
                Class<?> theClass = app().classForName(className);
                for (Map.Entry<String, String> entry : staticConfigurationFields.entrySet()) {
                    String fieldName = entry.getKey();
                    String conf = entry.getValue();
                    Field field = $.fieldOf(theClass, fieldName, false);
                    DefaultValue defaultValue = field.getAnnotation(DefaultValue.class);
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    boolean isConst = false;
                    boolean isVal = false;
                    BeanSpec valueSpec = BeanSpec.of(field, injector);
                    if (Const.class.isAssignableFrom(fieldType)) {
                        valueSpec = BeanSpec.of(field, injector).componentSpec();
                        isConst = true;
                    } else if ($.Val.class.isAssignableFrom(fieldType)) {
                        valueSpec = BeanSpec.of(field, injector).componentSpec();
                        isVal = true;
                    }
                    ConfigurationValueLoader loader = app().getInstance(ConfigurationValueLoader.class);
                    Map<String, String> map = C.newMap("value", conf);
                    if (null != defaultValue) {
                        map.put("defaultValue", defaultValue.value());
                    }
                    loader.init(map, valueSpec);
                    Object value = loader.get();
                    if (null == value) {
                        return;
                    }
                    try {
                        if (isConst) {
                            Const<?> fieldValue = $.cast(field.get(null));
                            if (null == fieldValue) {
                                fieldValue = $.constant(value);
                                field.set(null, fieldValue);
                            } else {
                                Field fv = Const.class.getDeclaredField("v");
                                fv.setAccessible(true);
                                fv.set(fieldValue, value);
                            }
                        } else if (isVal) {
                            $.Val<?> fieldValue = $.cast(field.get(null));
                            if (null == fieldValue) {
                                fieldValue = $.val(value);
                                field.set(null, fieldValue);
                            } else {
                                Field fv = $.Var.class.getDeclaredField("v");
                                fv.setAccessible(true);
                                fv.set(fieldValue, value);
                            }
                        } else {
                            field.set(null, value);
                        }
                    } catch (Exception e) {
                        throw E.unexpected(e, "failed to set configuration value[%s] to field[%s]", value, field);
                    }
                }
            }
        });
    }
}
