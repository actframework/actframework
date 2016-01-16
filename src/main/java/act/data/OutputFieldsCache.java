package act.data;

import act.app.App;
import act.data.util.StringOrPattern;
import act.util.ActContext;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.util.C;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

class OutputFieldsCache {

    // Key to index final output fields. Key includes:
    // 1. excluded - developer declared excluded field list
    // 2. outputs - developer declared output field list
    // 3. component type - the type of the entity where field data get extracted
    private class K {
        Set<String> excluded;
        List<String> outputs;
        Class<?> componentType;
        K(Set<String> ss, List<String> ls, Class<?> componentType) {
            excluded = ss;
            outputs = ls;
            this.componentType = componentType;
        }

        @Override
        public int hashCode() {
            return $.hc(excluded, outputs, componentType);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof K) {
                K that = (K) obj;
                return $.eq(that.excluded, this.excluded)
                        && $.eq(that.outputs, this.outputs)
                        && $.eq(that.componentType, this.componentType);
            }
            return false;
        }
    }

    private Map<K, List<String>> cache = C.newMap();

    public List<String> getOutputFields(PropertySpec.MetaInfo spec, Class<?> componentClass, ActContext context) {
        K k = new K(spec.excludedFields(context), spec.outputFields(context), componentClass);
        List<String> outputs = cache.get(k);
        if (null == outputs) {
            outputs = calculateOutputs(k);
            cache.put(k, outputs);
        }
        return outputs;
    }

    List<String> calculateOutputs(K k) {
        C.List<StringOrPattern> outputs = C.newList();
        boolean hasPattern = hasPattern(k.outputs, outputs);
        Set<String> excluded = k.excluded;
        if (hasPattern || !excluded.isEmpty()) {
            DataPropertyRepository repo = App.instance().service(DataPropertyRepository.class);
            List<String> allFields = repo.propertyListOf(k.componentType);
            if (!excluded.isEmpty()) {
                List<String> finalOutputs;
                List<StringOrPattern> lsp = C.newList();
                boolean excludeHasPattern = hasPattern(excluded, lsp);
                if (!excludeHasPattern) {
                    return C.list(allFields).without(excluded);
                } else {
                    finalOutputs = C.newList(allFields);
                    outer:
                    for (String s : allFields) {
                        for (StringOrPattern sp: lsp) {
                            if (sp.matches(s)) {
                                finalOutputs.remove(s);
                                continue  outer;
                            }
                        }
                    }
                    return finalOutputs;
                }
            } else { // excluded is empty and output fields has pattern
                List<String> finalOutputs = C.newList();
                for (StringOrPattern sp: outputs) {
                    if (sp.isPattern()) {
                        Pattern p = sp.p();
                        for (String s: allFields) {
                            if (p.matcher(s).matches()) {
                                finalOutputs.add(s);
                            }
                        }
                    } else {
                        finalOutputs.add(sp.s());
                    }
                }
                return finalOutputs;
            }
        } else {
            return k.outputs;
        }
    }

    private boolean hasPattern(Collection<String> ls, List<StringOrPattern> lsp) {
        boolean b = false;
        for (String s: ls) {
            StringOrPattern sp = new StringOrPattern(s);
            b = b || sp.isPattern();
            lsp.add(sp);
        }
        return b;
    }


}
