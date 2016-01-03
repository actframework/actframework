package act.cli.util;

import com.alibaba.fastjson.serializer.NameFilter;
import org.osgl.util.C;

import java.util.Map;

public class MappedFastJsonNameFilter implements NameFilter {

    private Map<String, String> nameMaps = C.newMap();

    /**
     * Construct a {@code MappedFastJsonNameFilter} with prop-name mapping
     * @param mapping the mapping from property to property name (label)
     */
    public MappedFastJsonNameFilter(Map<String, String> mapping) {
        nameMaps.putAll(mapping);
    }

    @Override
    public String process(Object object, String name, Object value) {
        String label = nameMaps.get(name);
        return null == label ? name : label;
    }

    public void addMap(String property, String label) {
        nameMaps.put(property, label);
    }

    public boolean isEmpty() {
        return nameMaps.isEmpty();
    }
}
