package act.job.meta;

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

import act.Act;
import act.app.App;
import act.app.event.SysEventId;
import act.asm.Type;
import act.event.meta.SimpleEventListenerMetaInfo;
import act.sys.meta.InvokeType;
import act.sys.meta.ReturnTypeInfo;
import act.util.*;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JobMethodMetaInfo extends LogSupportedDestroyableBase {

    private static final AtomicInteger jobIdCounter = new AtomicInteger(0);

    private String id;
    private String name;
    private App app;
    private InvokeType invokeType;
    private JobClassMetaInfo clsInfo;
    private ReturnTypeInfo returnType = new ReturnTypeInfo();
    private List<BeanSpec> paramTypes;
    private Method method;

    public JobMethodMetaInfo(final JobClassMetaInfo clsInfo, final List<String> paramTypes) {
        this.clsInfo = clsInfo;
        app = Act.app();
        app.jobManager().on(SysEventId.DEPENDENCY_INJECTOR_PROVISIONED, "JobMethodMetaInfo:init-" + jobIdCounter.getAndIncrement(), new Runnable() {
            @Override
            public void run() {
                $.Var<Method> var = $.var();
                JobMethodMetaInfo.this.paramTypes = SimpleEventListenerMetaInfo.convert(paramTypes, clsInfo.className(), name, var);
                JobMethodMetaInfo.this.method = var.get();

            }
        });
    }

    private JobMethodMetaInfo(final JobClassMetaInfo clsInfo, JobMethodMetaInfo parent) {
        this.app = parent.app;
        this.clsInfo = clsInfo;
        this.paramTypes = parent.paramTypes;
    }

    @Override
    protected void releaseResources() {
        app = null;
        clsInfo.destroy();
        super.releaseResources();
    }

    public JobClassMetaInfo classInfo() {
        return clsInfo;
    }

    public JobMethodMetaInfo name(String name) {
        this.name = name;
        return this;
    }

    public String name() {
        return name;
    }

    public String fullName() {
        return S.concat(clsInfo.className(), ".", name());
    }

    public JobMethodMetaInfo id(String id) {
        this.id = id;
        return this;
    }

    public String id() {
        return S.blank(id) ? fullName() : id;
    }

    public Method method() {
        if (null == method) {
            Class<?> c = app.classForName(classInfo().className());
            if (null == paramTypes() || paramTypes().isEmpty()) {
                method = $.getMethod(c, name());
            } else {
                throw new IllegalStateException("method cannot have parameters for Job invoked before app fully loaded");
            }
        }
        return method;
    }

    public JobMethodMetaInfo invokeStaticMethod() {
        invokeType = InvokeType.STATIC;
        return this;
    }

    public JobMethodMetaInfo invokeInstanceMethod() {
        invokeType = InvokeType.VIRTUAL;
        return this;
    }

    public boolean isStatic() {
        return InvokeType.STATIC == invokeType;
    }

    public JobMethodMetaInfo returnType(Type type) {
        returnType = ReturnTypeInfo.of(type);
        return this;
    }

    public Type returnType() {
        return returnType.type();
    }

    public List<BeanSpec> paramTypes() {
        return paramTypes;
    }

    @Override
    public int hashCode() {
        return $.hc(fullName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof JobMethodMetaInfo) {
            JobMethodMetaInfo that = (JobMethodMetaInfo) obj;
            return $.eq(that.fullName(), fullName());
        }
        return false;
    }

    @Override
    public String toString() {
        S.Buffer sb = S.newBuffer();
        sb.append(_invokeType())
                .append(_return())
                .append(fullName());
        return sb.toString();
    }

    public List<JobMethodMetaInfo> extendedJobMethodMetaInfoList(App app) {
        E.illegalStateIf(!classInfo().isAbstract(), "this job method meta info is not abstract");
        final List<JobMethodMetaInfo> list = new ArrayList<>();
        final JobClassMetaInfo clsInfo = classInfo();
        String clsName = clsInfo.className();
        ClassNode node = app.classLoader().classInfoRepository().node(clsName);
        if (null == node) {
            return list;
        }
        final JobMethodMetaInfo me = this;
        node.visitTree(new $.Visitor<ClassNode>() {
            @Override
            public void visit(ClassNode classNode) throws $.Break {
                if (!classNode.isAbstract() && classNode.isPublic()) {
                    JobClassMetaInfo subClsInfo = new JobClassMetaInfo().className(classNode.name());
                    JobMethodMetaInfo subMethodInfo = new JobMethodMetaInfo(subClsInfo, JobMethodMetaInfo.this);
                    if (me.isStatic()) {
                        subMethodInfo.invokeStaticMethod();
                    } else {
                        subMethodInfo.invokeInstanceMethod();
                    }
                    subMethodInfo.name(me.name());
                    subMethodInfo.returnType(me.returnType());
                    list.add(subMethodInfo);
                }
            }
        });
        return list;
    }

    private String _invokeType() {
        if (null == invokeType) {
            return "";
        }
        switch (invokeType) {
            case VIRTUAL:
                return "";
            case STATIC:
                return "static ";
            default:
                assert false;
                return "";
        }
    }

    private String _return() {
        if (null == returnType) {
            return " ";
        }
        if (returnType.hasReturn()) {
            return returnType.type().getClassName() + " ";
        } else {
            return "";
        }
    }

}
