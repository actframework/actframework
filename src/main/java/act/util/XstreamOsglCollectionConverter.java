package act.util;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import org.osgl.$;
import org.osgl.Osgl;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.SetBase;
import org.osgl.util.TraversableBase;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class XstreamOsglCollectionConverter extends AbstractCollectionConverter {
    public XstreamOsglCollectionConverter(Mapper mapper) {
        super(mapper);
    }

    @Override
    public boolean canConvert(Class type) {
        return C.List.class.isAssignableFrom(type) || SetBase.class.isAssignableFrom(type) || C.ListOrSet.class.isAssignableFrom(type);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Iterable collection = $.cast(source);
        for (Iterator iterator = collection.iterator(); iterator.hasNext(); ) {
            Object item = iterator.next();
            writeItem(item, context, writer);
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Class c = context.getRequiredType();
        String cn = c.getName();
        Collection collection;
        if (C.List.class.isAssignableFrom(c)) {
            collection = C.newList();
        } else if (SetBase.class.isAssignableFrom(c)) {
            collection = C.newSet();
        } else if (C.ListOrSet.class.isAssignableFrom(c)) {
            if (cn.contains("Val")) {
                reader.moveDown();
                Object item = readItem(reader, context, C.newList());
                return Osgl.Val.of(item);
            } else if (cn.contains("Var")) {
                reader.moveDown();
                Object item = readItem(reader, context, C.newList());
                return Osgl.Var.of(item);
            } else {
                return C.empty();
            }
        } else {
            throw E.unexpected("Unknown collection type: %s", c);
        }
        populateCollection(reader, context, collection);
        return collection;
    }


    protected void populateCollection(HierarchicalStreamReader reader, UnmarshallingContext context, Collection collection) {
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            Object item = readItem(reader, context, collection);
            collection.add(item);
            reader.moveUp();
        }
    }

}
