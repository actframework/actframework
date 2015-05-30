package act.http;

import java.util.Map;

// Disclaim: the code come from PlayFramework
public class MapUtil {

    public static void mergeValueInMap(Map<String, String[]> map, String name, String value) {
        String[] newValues;
        String[] oldValues = map.get(name);
        if (oldValues == null) {
            newValues = new String[1];
            newValues[0] = value;
        } else {
            newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }
        map.put(name, newValues);
    }

    public static void mergeValueInMap(Map<String, String[]> map, String name, String[] values) {
        for (String value : values) {
            mergeValueInMap(map, name, value);
        }
    }

    public static <K, V> Map<K, V> filterMap(Map<K, V> map, String keypattern) {
        try {
            @SuppressWarnings("unchecked")
            Map<K, V> filtered = map.getClass().newInstance();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                K key = entry.getKey();
                if (key.toString().matches(keypattern)) {
                    filtered.put(key, entry.getValue());
                }
            }
            return filtered;
        } catch (Exception iex) {
            return null;
        }
    }
}
