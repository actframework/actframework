package act.inject.param;

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

import act.inject.DefaultValue;
import org.osgl.mvc.annotation.Param;
import org.osgl.util.E;
import org.osgl.util.StringValueResolver;

abstract class StringValueResolverValueLoaderBase implements ParamValueLoader {

    protected final StringValueResolver<?> stringValueResolver;
    protected final ParamKey paramKey;
    protected final Object defVal;
    protected final DefaultValue defSpec;


    public StringValueResolverValueLoaderBase(ParamKey key, StringValueResolver<?> resolver, Param param, DefaultValue def, Class<?> type, boolean simpleKeyOnly) {
        E.illegalArgumentIf(simpleKeyOnly && !key.isSimple());
        this.paramKey = key;
        this.stringValueResolver = resolver;
        this.defSpec = def;
        Object _defVal = defVal(param, type);
        if (null == _defVal && null != def) {
            _defVal = resolver.resolve(def.value());
        }
        this.defVal = _defVal;
    }

    @Override
    public String bindName() {
        return paramKey.toString();
    }

    static Object defVal(Param param, Class<?> rawType) {
        if (boolean.class == rawType) {
            return null != param && param.defBooleanVal();
        } else if (int.class == rawType) {
            return null != param ? param.defIntVal() : 0;
        } else if (double.class == rawType) {
            return null != param ? param.defDoubleVal() : 0d;
        } else if (long.class == rawType) {
            return null != param ? param.defLongVal() : 0L;
        } else if (float.class == rawType) {
            return null != param ? param.defFloatVal() : 0f;
        } else if (char.class == rawType) {
            return null != param ? param.defCharVal() : '\0';
        } else if (byte.class == rawType) {
            return null != param ? param.defByteVal() : 0;
        } else if (short.class == rawType) {
            return null != param ? param.defShortVal() : 0;
        }
        return null;
    }
}
