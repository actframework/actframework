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

import act.Act;
import act.app.event.SysEventId;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.*;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.*;
import org.osgl.$;
import org.osgl.util.BeanInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FastJsonEnhancedAdaptiveMapCodec extends SerializeFilterable implements ObjectDeserializer, ObjectSerializer{

    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        final JSONLexer lexer = parser.lexer;
        if (lexer.token() == JSONToken.NULL) {
            lexer.nextToken(JSONToken.COMMA);
            return null;
        }

        EnhancedAdaptiveMap ar = Act.app().getInstance((Class<EnhancedAdaptiveMap>) type);

        ParseContext context = parser.getContext();

        try {
            parser.setContext(context, ar, fieldName);
            return (T) parseActiveBean(parser, ar, fieldName);
        } finally {
            parser.setContext(context);
        }
    }

    @SuppressWarnings("rawtypes")
    public static EnhancedAdaptiveMap parseActiveBean(DefaultJSONParser parser, EnhancedAdaptiveMap ar, Object fieldName) {
        JSONLexer lexer = parser.lexer;

        if (lexer.token() != JSONToken.LBRACE) {
            throw new JSONException("syntax error, expect {, actual " + lexer.token());
        }

        ParseContext context = parser.getContext();
        try {
            for (int i = 0;;++i) {
                lexer.skipWhitespace();
                char ch = lexer.getCurrent();
                if (lexer.isEnabled(Feature.AllowArbitraryCommas)) {
                    while (ch == ',') {
                        lexer.next();
                        lexer.skipWhitespace();
                        ch = lexer.getCurrent();
                    }
                }

                String key;
                if (ch == '"') {
                    key = lexer.scanSymbol(parser.getSymbolTable(), '"');
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new JSONException("expect ':' at " + lexer.pos());
                    }
                } else if (ch == '}') {
                    lexer.next();
                    lexer.resetStringPosition();
                    lexer.nextToken(JSONToken.COMMA);
                    return ar;
                } else if (ch == '\'') {
                    if (!lexer.isEnabled(Feature.AllowSingleQuotes)) {
                        throw new JSONException("syntax error");
                    }

                    key = lexer.scanSymbol(parser.getSymbolTable(), '\'');
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new JSONException("expect ':' at " + lexer.pos());
                    }
                } else {
                    if (!lexer.isEnabled(Feature.AllowUnQuotedFieldNames)) {
                        throw new JSONException("syntax error");
                    }

                    key = lexer.scanSymbolUnQuoted(parser.getSymbolTable());
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new JSONException("expect ':' at " + lexer.pos() + ", actual " + ch);
                    }
                }

                lexer.next();
                lexer.skipWhitespace();
                lexer.getCurrent();

                lexer.resetStringPosition();

                Object value;
                lexer.nextToken();

                if (i != 0) {
                    parser.setContext(context);
                }

                if (lexer.token() == JSONToken.NULL) {
                    value = null;
                    lexer.nextToken();
                } else {
                    value = parser.parseObject(ar.metaInfo().setterFieldType(key), key);
                }

                ar.putValue(key, value);
                parser.checkMapResolve(ar.asMap(), key);

                parser.setContext(context, value, key);
                parser.setContext(context);

                final int tok = lexer.token();
                if (tok == JSONToken.EOF || tok == JSONToken.RBRACKET) {
                    return ar;
                }

                if (tok == JSONToken.RBRACE) {
                    lexer.nextToken();
                    return ar;
                }
            }
        } finally {
            parser.setContext(context);
        }

    }

    private static $.Predicate<BeanInfo> fieldFilter = new $.Predicate<BeanInfo>() {
        @Override
        public boolean test(BeanInfo beanSpec) {
            return !beanSpec.isTransient();
        }
    };

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;

        if (object == null) {
            out.writeNull();
            return;
        }

        EnhancedAdaptiveMap<?> ar = (EnhancedAdaptiveMap) object;

        if (serializer.containsReference(object)) {
            serializer.writeReference(object);
            return;
        }

        SerialContext parent = serializer.getContext();
        serializer.setContext(parent, object, fieldName, 0);
        try {
            out.write('{');

            serializer.incrementIndent();

            Class<?> preClazz = null;
            ObjectSerializer preWriter = null;

            boolean first = true;

            if (out.isEnabled(SerializerFeature.WriteClassName)) {
                String typeKey = serializer.getMapping().getTypeKey();
                Class<?> mapClass = ar.getClass();
                boolean containsKey = (mapClass == JSONObject.class || mapClass == HashMap.class || mapClass == LinkedHashMap.class)
                        && ar.containsKey(typeKey);
                if (!containsKey) {
                    out.writeFieldName(typeKey);
                    out.writeString(object.getClass().getName());
                    first = false;
                }
            }

            for (Map.Entry<String, Object> entry : ar.entrySet(fieldFilter)) {
                Object value = entry.getValue();

                String entryKey = entry.getKey();

                {
                    List<PropertyPreFilter> preFilters = serializer.getPropertyPreFilters();
                    if (preFilters != null && preFilters.size() > 0) {
                        if (!this.applyName(serializer, object, entryKey)) {
                            continue;
                        }
                    }
                }
                {
                    List<PropertyPreFilter> preFilters = this.propertyPreFilters;
                    if (preFilters != null && preFilters.size() > 0) {
                        if (!this.applyName(serializer, object, entryKey)) {
                            continue;
                        }
                    }
                }

                {
                    List<PropertyFilter> propertyFilters = serializer.getPropertyFilters();
                    if (propertyFilters != null && propertyFilters.size() > 0) {
                        if (!this.apply(serializer, object, entryKey, value)) {
                            continue;
                        }
                    }
                }
                {
                    List<PropertyFilter> propertyFilters = this.propertyFilters;
                    if (propertyFilters != null && propertyFilters.size() > 0) {
                        if (!this.apply(serializer, object, entryKey, value)) {
                            continue;
                        }
                    }
                }

                {
                    List<NameFilter> nameFilters = serializer.getNameFilters();
                    if (nameFilters != null && nameFilters.size() > 0) {
                        entryKey = this.processKey(serializer, object, entryKey, value);
                    }
                }
                {
                    List<NameFilter> nameFilters = this.nameFilters;
                    if (nameFilters != null && nameFilters.size() > 0) {
                        entryKey = this.processKey(serializer, object, entryKey, value);
                    }
                }

                {
                    List<ValueFilter> valueFilters = serializer.getValueFilters();
                    List<ContextValueFilter> contextValueFilters = this.contextValueFilters;
                    if ((valueFilters != null && valueFilters.size() > 0) //
                            || (contextValueFilters != null && contextValueFilters.size() > 0)) {
                        value = this.processValue(serializer, null, object, entryKey, value, features);
                    }
                }
                {
                    List<ValueFilter> valueFilters = this.valueFilters;
                    List<ContextValueFilter> contextValueFilters = this.contextValueFilters;
                    if ((valueFilters != null && valueFilters.size() > 0) //
                            || (contextValueFilters != null && contextValueFilters.size() > 0)) {
                        value = this.processValue(serializer, null, object, entryKey, value, features);
                    }
                }

                if (value == null) {
                    if (!out.isEnabled(SerializerFeature.WRITE_MAP_NULL_FEATURES)) {
                        continue;
                    }
                }

                String key = entryKey;

                if (!first) {
                    out.write(',');
                }

                if (out.isEnabled(SerializerFeature.PrettyFormat)) {
                    serializer.println();
                }
                out.writeFieldName(key, true);

                first = false;

                if (value == null) {
                    out.writeNull();
                    continue;
                }

                Class<?> clazz = value.getClass();

                if (clazz == preClazz) {
                    preWriter.write(serializer, value, entryKey, null, 0);
                } else {
                    preClazz = clazz;
                    preWriter = serializer.getObjectWriter(clazz);

                    preWriter.write(serializer, value, entryKey, null, 0);
                }
            }
        } finally {
            serializer.setContext(parent);
        }

        serializer.decrementIdent();
        if (out.isEnabled(SerializerFeature.PrettyFormat) && ar.size() > 0) {
            serializer.println();
        }
        out.write('}');
    }

    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }

    @SubClassFinder(callOn = SysEventId.DEPENDENCY_INJECTOR_PROVISIONED)
    public static void foundEnhancedAdaptiveMapClass(Class<? extends EnhancedAdaptiveMap> clazz) {

        FastJsonEnhancedAdaptiveMapCodec codec = new FastJsonEnhancedAdaptiveMapCodec();

        SerializeConfig config = SerializeConfig.getGlobalInstance();
        config.put(clazz, codec);

        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
        parserConfig.putDeserializer(clazz, codec);
    }

}
