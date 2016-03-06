package act.util;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import org.osgl.util.C;

import java.util.Map;

public class XstreamOsglMapConverter extends MapConverter {

    public XstreamOsglMapConverter(Mapper mapper) {
        super(mapper);
    }

    public XstreamOsglMapConverter(Mapper mapper, Class type) {
        super(mapper, type);
    }

    @Override
    public boolean canConvert(Class type) {
        return super.canConvert(type) || C.Map.class.isAssignableFrom(type);
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Class c = context.getRequiredType();
        if (C.Map.class.isAssignableFrom(c)) {
            Map map = C.newMap();
            populateMap(reader, context, map);
            return map;
        } else {
            return super.unmarshal(reader, context);
        }
    }
}
