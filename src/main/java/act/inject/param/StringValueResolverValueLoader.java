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

import act.app.ActionContext;
import act.inject.DefaultValue;
import act.util.ActContext;
import org.osgl.mvc.annotation.Param;
import org.osgl.util.E;
import org.osgl.util.StringValueResolver;

public class StringValueResolverValueLoader extends StringValueResolverValueLoaderBase implements ParamValueLoader {

    private static final ThreadLocal<HttpRequestParamEncode> encodeShare = new ThreadLocal<HttpRequestParamEncode>();

    private HttpRequestParamEncode encode;

    public StringValueResolverValueLoader(ParamKey key, StringValueResolver<?> resolver, Param param, DefaultValue def, Class<?> type) {
        super(key, resolver, param, def, type, false);
    }

    @Override
    public Object load(Object bean, ActContext<?> context, boolean noDefaultValue) {
        if (paramKey.isSimple()) {
            String value = context.paramVal(paramKey.name());
            Object obj = (null == value) ? null : stringValueResolver.resolve(value);
            return (null == obj) && !noDefaultValue ? defVal : obj;
        }
        ParamTree paramTree = ParamValueLoaderService.paramTree();
        if (null != paramTree) {
            return load(paramTree, context);
        }
        HttpRequestParamEncode encode = encodeShare.get();
        if (null == encode) {
            encode = this.encode;
            if (null == encode) {
                encode = save(HttpRequestParamEncode.JQUERY);
            }
        }
        Object obj = load(context, encode);
        if (null == obj) {
            HttpRequestParamEncode encode0 = encode;
            do {
                encode0 = HttpRequestParamEncode.next(encode0);
                obj = load(context, encode0);
            } while (null == obj && encode0 != encode);
            if (null != obj) {
                save(encode0);
            }
        }
        return null == obj && !noDefaultValue ? defVal : obj;
    }

    private Object load(ActContext context, HttpRequestParamEncode encode) {
        String bindName = encode.concat(paramKey);
        String value = context.paramVal(bindName);
        return (null == value) ? null : stringValueResolver.resolve(value);
    }

    private Object load(ParamTree tree, ActContext context) {
        ParamTreeNode node = tree.node(paramKey);
        if (null == node) {
            if (context instanceof ActionContext) {
                ActionContext actx = (ActionContext) context;
                if (actx.isAllowIgnoreParamNamespace()) {
                    ParamKey withoutNamespace = paramKey.withoutNamespace();
                    if (null != withoutNamespace) {
                        node = tree.node(withoutNamespace);
                    }
                }
            }
            if (null == node) {
                return null;
            }
        }
        if (!node.isLeaf()) {
            throw E.unexpected("Expect leaf node, found: \n%s", node.debug());
        }
        String value = node.value();
        return (null == value) ? null : stringValueResolver.resolve(value);
    }

    private HttpRequestParamEncode save(HttpRequestParamEncode encode) {
        this.encode = encode;
        encodeShare.set(encode);
        return encode;
    }

}
