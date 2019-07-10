package act.controller.meta;

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

import act.asm.Label;
import act.asm.Type;
import act.handler.builtin.controller.ControllerAction;
import act.handler.builtin.controller.Handler;
import act.sys.meta.InvokeType;
import act.sys.meta.ReturnTypeInfo;
import act.util.*;
import org.osgl.$;
import org.osgl.util.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Common meta data storage for both {@link ControllerAction}
 * and {@link Handler}
 */
public abstract class HandlerMethodMetaInfo<T extends HandlerMethodMetaInfo> extends LogSupportedDestroyableBase implements Prioritised {

    private String name;
    private InvokeType invokeType;
    private ActContextInjection actContextInjection;
    private ControllerClassMetaInfo clsInfo;
    private C.List<HandlerParamMetaInfo> params = C.newList();
    private volatile String fullName;
    private ReturnTypeInfo returnType;
    private String dateFormatPattern;
    private boolean returnArray; // for performance tuning
    private boolean throwRenderResult;
    private PropertySpec.MetaInfo propertySpec;
    private Map<Label, Map<Integer, LocalVariableMetaInfo>> locals = new HashMap<>();
    private int appCtxLVT_id = -1;
    private int ctxParamCnt = -1;

    /**
     * Construct a `HandlerMethodMetaInfo` from a copy with a different class info. This could be used
     * to get Interceptors from parent controller
     *
     * @param copy an existing HandlerMethodMetaInfo
     * @param clsInfo the new class info, usually a extended controller class
     */
    protected HandlerMethodMetaInfo(HandlerMethodMetaInfo copy, ControllerClassMetaInfo clsInfo) {
        E.illegalArgumentIf(!clsInfo.isMyAncestor(copy.classInfo()));
        this.clsInfo = $.requireNotNull(clsInfo);
        this.name = copy.name;
        this.invokeType = copy.invokeType;
        this.actContextInjection = copy.actContextInjection;
        this.params = copy.params;
        this.returnType = copy.returnType;
        this.returnArray = copy.returnArray;
        this.propertySpec = copy.propertySpec;
        this.locals = copy.locals;
        this.appCtxLVT_id = copy.appCtxLVT_id;
        this.ctxParamCnt = copy.ctxParamCnt;
    }

    public HandlerMethodMetaInfo(ControllerClassMetaInfo clsInfo) {
        this.clsInfo = clsInfo;
    }

    @Override
    protected void releaseResources() {
        clsInfo.destroy();
        params.clear();
        locals.clear();
        super.releaseResources();
    }

    public ControllerClassMetaInfo classInfo() {
        return clsInfo;
    }

    public T name(String name) {
        this.name = name;
        return me();
    }

    public String name() {
        return name;
    }

    public String fullName() {
        if (null == fullName) {
            synchronized (this) {
                if (null == fullName) {
                    fullName = S.concat(classInfo().className(), ":", name);
                }
            }
        }
        return fullName;
    }

    public T dateFormatPattern(String pattern) {
        this.dateFormatPattern = pattern;
        return me();
    }

    public String dateFormatPattern() {
        return this.dateFormatPattern;
    }

    @Override
    public Integer priority() {
        return null;
    }

    public T appContextViaField(String fieldName) {
        actContextInjection = new ActContextInjection.FieldActContextInjection(fieldName);
        return me();
    }

    public T appContextViaParam(int paramIndex) {
        actContextInjection = new ActContextInjection.ParamAppContextInjection(paramIndex);
        return me();
    }

    public T appContextViaLocalStorage() {
        actContextInjection = new ActContextInjection.LocalAppContextInjection();
        return me();
    }

    public ActContextInjection appContextInjection() {
        return actContextInjection;
    }

    public T invokeStaticMethod() {
        invokeType = InvokeType.STATIC;
        return me();
    }

    public T invokeInstanceMethod() {
        invokeType = InvokeType.VIRTUAL;
        return me();
    }

    public boolean isStatic() {
        return InvokeType.STATIC == invokeType;
    }

    public HandlerMethodMetaInfo propertySpec(PropertySpec.MetaInfo propertySpec) {
        this.propertySpec = propertySpec;
        return this;
    }

    public PropertySpec.MetaInfo propertySpec() {
        return propertySpec;
    }

    public T setThrowRenderResult() {
        this.throwRenderResult = true;
        return me();
    }

    public boolean throwRenderResult() {
        return throwRenderResult;
    }

    public T returnType(Type type) {
        returnType = ReturnTypeInfo.of(type);
        returnArray = type.getDescriptor().startsWith("[");
        return me();
    }

    public T appCtxLocalVariableTableIndex(int index) {
        appCtxLVT_id = index;
        return me();
    }

    public int appCtxLocalVariableTableIndex() {
        return appCtxLVT_id;
    }

    public Type returnType() {
        return returnType.type();
    }

    public boolean isReturnArray() {
        return returnArray;
    }

    public ReturnTypeInfo returnTypeInfo() {
        return returnType;
    }

    public Type returnComponentType() {
        return returnType.componentType();
    }

    public HandlerMethodMetaInfo returnComponentType(Type type) {
        returnType.componentType(type);
        return this;
    }

    public boolean hasReturn() {
        return returnType.hasReturn();
    }

    public boolean hasReturnOrThrowResult() {
        return hasReturn() || throwRenderResult;
    }

    public boolean hasLocalVariableTable() {
        return !locals.isEmpty();
    }

    public HandlerMethodMetaInfo addParam(HandlerParamMetaInfo param) {
        params.add(param);
        return this;
    }

    public T addLocal(LocalVariableMetaInfo local) {
        Label start = local.start();
        Map<Integer, LocalVariableMetaInfo> m = locals.get(start);
        if (null == m) {
            m = new HashMap<>();
            locals.put(start, m);
        }
        int index = local.index();
        E.illegalStateIf(m.containsKey(index), "Local variable index conflict");
        m.put(local.index(), local);
        return me();
    }

    public LocalVariableMetaInfo localVariable(int index, Label start) {
        Map<Integer, LocalVariableMetaInfo> l = locals.get(start);
        if (null == l) return null;
        return l.get(index);
    }

    public HandlerParamMetaInfo param(int id) {
        return params.get(id);
    }

    public int paramCount() {
        return params.size();
    }

    public synchronized int ctxParamCount() {
        if (ctxParamCnt < 0) {
            if (paramCount() == 0) {
                ctxParamCnt = 0;
            } else {
                ctxParamCnt = 0;
                for (HandlerParamMetaInfo param : params) {
                    if (param.isContext()) {
                        ctxParamCnt ++;
                    }
                }
            }
        }
        return ctxParamCnt;
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
        if (obj instanceof HandlerMethodMetaInfo) {
            HandlerMethodMetaInfo that = (HandlerMethodMetaInfo) obj;
            return $.eq(that.fullName(), fullName());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStrBuffer(S.newBuffer()).toString();
    }

    protected S.Buffer toStrBuffer(S.Buffer sb) {
        sb.append(actContextInjection).append(" ");
        sb.append(_invokeType())
                .append(_return())
                .append(fullName())
                .append("(")
                .append(_params())
                .append(")");
        return sb;
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
            return "";
        }
        if (returnType.hasReturn()) {
            return returnType.type().getClassName() + " ";
        } else {
            return "";
        }
    }


    private String _params() {
        return S.join(", ", params.map(new $.Transformer<HandlerParamMetaInfo, String>() {
            @Override
            public String transform(HandlerParamMetaInfo paramMetaInfo) {
                return paramMetaInfo.type().getClassName();
            }
        }));
    }

    private T me() {
        return (T) this;
    }

}
