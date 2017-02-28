package act.util;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import org.osgl.util.Keyword;
import org.osgl.util.KeywordValueObjectCodec;

import java.io.IOException;
import java.lang.reflect.Type;

public class FastJsonKeywordCodec extends SingletonBase implements ObjectSerializer, ObjectDeserializer {

    private KeywordValueObjectCodec keywordCodec;

    public FastJsonKeywordCodec() {
        keywordCodec = KeywordValueObjectCodec.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JSONLexer lexer = parser.getLexer();
        if (lexer.token() == JSONToken.LITERAL_STRING) {
            String text = lexer.stringVal();
            lexer.nextToken(JSONToken.COMMA);
            return (T) Keyword.of(text);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public int getFastMatchToken() {
        return JSONToken.LITERAL_STRING;
    }

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.getWriter();

        if (object == null) {
            out.writeNull();
            return;
        }

        out.writeString(keywordCodec.toString((Keyword) object));
    }

}
