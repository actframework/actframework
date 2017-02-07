package act.util;

import act.data.SObjectResolver;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;
import org.osgl.util.Codec;
import org.osgl.util.Keyword;
import org.osgl.util.KeywordValueObjectCodec;

import java.io.IOException;
import java.lang.reflect.Type;

public class FastJsonSObjectCodec extends SingletonBase implements ObjectDeserializer {

    private SObjectResolver resolver;

    public FastJsonSObjectCodec() {
        resolver = SObjectResolver.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JSONLexer lexer = parser.getLexer();
        if (lexer.token() == JSONToken.LITERAL_STRING) {
            String text = lexer.stringVal();
            lexer.nextToken();
            return (T) resolver.resolve(text);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public int getFastMatchToken() {
        return JSONToken.LITERAL_STRING;
    }

}
