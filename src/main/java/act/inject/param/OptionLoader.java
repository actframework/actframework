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

import act.Act;
import act.cli.*;
import act.inject.DefaultValue;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.*;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * Load command line options
 */
class OptionLoader extends CliParamValueLoader {

    final String bindName;
    String lead1;
    String lead2;
    final String defVal;
    final Object langDefVal;
    final String requiredGroup;
    final boolean required;
    final BeanSpec beanSpec;
    final String errorTemplate;
    private final StringValueResolver resolver;

    OptionLoader(String bindName, Optional optional, StringValueResolver resolver, BeanSpec beanSpec) {
        this.bindName = bindName;
        this.required = false;
        this.parseLeads(optional.lead());
        String defVal = optional.defVal();
        this.requiredGroup = null;
        this.beanSpec = beanSpec;
        this.errorTemplate = errorTemplate(optional);
        this.resolver = resolver;
        CliContext.ParsingContextBuilder.foundOptional();
        if (S.blank(defVal)) {
            DefaultValue defaultValue = beanSpec.getAnnotation(DefaultValue.class);
            if (null != defaultValue) {
                defVal = defaultValue.value();
            }
        }
        this.defVal = defVal;
        Class<?> rawType = beanSpec.rawType();
        if (rawType.isArray()) {
            this.langDefVal = Array.newInstance(rawType.getComponentType(), 0);
        } else if ($.isPrimitiveType(rawType)) {
            this.langDefVal = $.primitiveDefaultValue(rawType);
        } else if (Collection.class.isAssignableFrom(rawType)) {
            this.langDefVal = Act.getInstance(rawType);
        } else {
            this.langDefVal = null;
        }
    }

    OptionLoader(String bindName, Required required, StringValueResolver resolver, BeanSpec beanSpec) {
        this.bindName = bindName;
        this.required = true;
        this.parseLeads(required.lead());
        String group = required.group();
        this.requiredGroup = S.blank(group) ? bindName : group;
        this.beanSpec = beanSpec;
        this.errorTemplate = errorTemplate(required);
        this.resolver = resolver;
        CliContext.ParsingContextBuilder.foundRequired(this.requiredGroup);
        DefaultValue defaultValue = beanSpec.getAnnotation(DefaultValue.class);
        this.defVal = null == defaultValue ? null : defaultValue.value();
        Class<?> rawType = beanSpec.rawType();
        if (rawType.isArray()) {
            this.langDefVal = Array.newInstance(rawType.getComponentType(), 0);
        } else if ($.isPrimitiveType(rawType)) {
            this.langDefVal = $.primitiveDefaultValue(rawType);
        } else if (Collection.class.isAssignableFrom(rawType)) {
            this.langDefVal = Act.getInstance(rawType);
        } else {
            this.langDefVal = null;
        }
    }

    @Override
    public String toString() {
        return S.concat("cli option loader[", this.bindName, "]");
    }

    @Override
    public Object load(Object cachedBean, ActContext<?> context, boolean noDefaultValue) {
        CliContext ctx = (CliContext) context;
        String optVal = ctx.paramVal(bindName);
        if (S.blank(optVal)) {
            optVal = this.defVal;
        }
        if (S.blank(optVal) && required) {
            optVal = getFirstArgument(ctx);
        }
        Object val = null;
        if (S.notBlank(optVal)) {
            val = resolve(optVal);
        }
        if (null == val && null != cachedBean) {
            val = cachedBean;
        }
        if (null != val && required) {
            ctx.parsingContext().foundRequired(requiredGroup);
        }
        if (null == val) {
            val = langDefVal;
        }
        return val;
    }

    @Override
    public String bindName() {
        return this.bindName;
    }

    private Object resolve(String val) {
        return resolve(val, resolver);
    }

    private <T> T resolve(String val, StringValueResolver<T> resolver) {
        if (null == errorTemplate) {
            return resolver.resolve(val);
        }
        try {
            return resolver.resolve(val);
        } catch (Exception e) {
            throw E.unexpected(errorTemplate, val);
        }
    }

    private String errorTemplate(Optional optional) {
        return verifyErrorTemplate(optional.errorTemplate());
    }

    private String errorTemplate(Required required) {
        return verifyErrorTemplate(required.errorTemplate());
    }

    private String verifyErrorTemplate(String s) {
        if (S.blank(s)) {
            return null;
        }
        if (!s.contains("%")) {
            throw E.invalidConfiguration("Error template must have format argument placeholder, e.g. %s inside it: " + s);
        }
        if (s.split("%").length > 2) {
            throw E.invalidConfiguration("Error template must not have  more than one format argument placeholder, e.g. %s inside it: " + s);
        }
        return s;
    }

    private void parseLeads(String[] specs) {
        lead1 = specs[0];
        if (specs.length > 1) {
            lead2 = specs[1];
        } else {
            String[] sa = lead1.split("[,;\\s]+");
            if (sa.length > 2) {
                throw E.unexpected("Option cannot have more than two leads");
            }
            if (sa.length > 1) {
                lead1 = sa[0];
                lead2 = sa[1];
            }
        }
        if (S.blank(lead1)) {
            lead1 = "-" + bindName.charAt(0);
            lead2 = "--" + Keyword.of(bindName).dashed();
        }
    }

}
