package act.data;

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

import act.app.App;
import act.data.util.StringOrPattern;
import act.util.ActContext;
import act.util.PropertySpec;
import org.osgl.$;
import org.osgl.util.AdaptiveMap;
import org.osgl.util.C;
import org.osgl.util.S;
import org.osgl.util.Str;

import java.util.*;
import java.util.regex.Pattern;

class OutputFieldsCache {

    // Key to index final output fields. Key includes:
    // 1. excluded - developer declared excluded field list
    // 2. outputs - developer declared output field list
    // 3. component type - the type of the entity where field data get extracted
    private class K {
        Set<String> excluded;
        List<S.Pair> outputs;
        Class<?> componentType;
        K(Set<String> ss, List<S.Pair> ls, Class<?> componentType) {
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

    private Map<K, List<S.Pair>> cache = new HashMap<>();

    public List<S.Pair> getOutputFields(PropertySpec.MetaInfo spec, Class<?> componentClass, Object firstElement, ActContext context) {
        K k = new K(spec.excludedFields(context), spec.outputFieldsAndLabel(context), componentClass);
        List<S.Pair> outputs = cache.get(k);
        if (null == outputs) {
            outputs = calculateOutputs(k, firstElement);
            cache.put(k, outputs);
        }
        return outputs;
    }

    List<S.Pair> calculateOutputs(K k, Object firstElement) {
        Class<?> type = k.componentType;
        if ($.isSimpleType(type) && k.excluded.isEmpty() && k.outputs.isEmpty()) {
            return C.list();
        }
        List<StringOrPattern> outputs = new ArrayList<>();
        boolean hasPattern = hasPattern2(k.outputs, outputs);
        Set<String> excluded = k.excluded;
        DataPropertyRepository repo = App.instance().service(DataPropertyRepository.class);
        List<S.Pair> allFields = repo.propertyListOf(k.componentType);
        if (AdaptiveMap.class.isInstance(firstElement)) {
            Set<String> mapped = new HashSet<>();
            for (S.Pair pair : allFields) {
                mapped.add(pair._1);
            }
            AdaptiveMap am = $.cast(firstElement);
            Map map = am.internalMap();
            for (Object o : map.keySet()) {
                String key = S.string(o);
                if (!mapped.contains(key)) {
                    allFields.add(S.pair(key, key));
                }
            }
        } else if (Map.class.isInstance(firstElement)) {
            Set<String> mapped = new HashSet<>();
            List<S.Pair> toBeRemoved = new ArrayList<>();
            for (S.Pair pair : allFields) {
                if ("empty".equalsIgnoreCase(pair._1)) {
                    toBeRemoved.add(pair);
                } else if ("innerMap.empty".equalsIgnoreCase(pair._1)) {
                    toBeRemoved.add(pair);
                } else {
                    mapped.add(pair._1);
                }
            }
            allFields.removeAll(toBeRemoved);
            Map map = $.cast(firstElement);
            for (Object o : map.keySet()) {
                String key = S.string(o);
                if (!mapped.contains(key)) {
                    allFields.add(S.pair(key, key));
                }
            }
        }
        if (hasPattern || outputs.isEmpty()) {
            if (!excluded.isEmpty()) {
                List<S.Pair> finalOutputs;
                List<StringOrPattern> lsp = new ArrayList<>();
                boolean excludeHasPattern = hasPattern(excluded, lsp);
                if (!excludeHasPattern) {
                    List<S.Pair> ret = new ArrayList<>(allFields);
                    for (S.Pair pair : allFields) {
                        for (String s : excluded) {
                            if (pair._1.equals(s)) {
                                ret.remove(pair);
                                break;
                            }
                        }
                    }
                    return ret;
                } else {
                    finalOutputs = C.newList(allFields);
                    outer:
                    for (S.Pair pair : allFields) {
                        for (StringOrPattern sp: lsp) {
                            if (sp.matches(pair._1)) {
                                finalOutputs.remove(pair);
                                continue  outer;
                            }
                        }
                    }
                    return finalOutputs;
                }
            } else {
                if (outputs.isEmpty()) {
                    return allFields;
                }
                // excluded is empty and output fields has pattern
                List<S.Pair> finalOutputs = new ArrayList<>();
                for (StringOrPattern sp: outputs) {
                    if (sp.isPattern()) {
                        Pattern p = sp.p();
                        for (S.Pair pair: allFields) {
                            if (p.matcher(pair._1).matches()) {
                                finalOutputs.add(pair);
                            }
                        }
                    } else {
                        for (S.Pair pair : allFields) {
                            if (S.eq(pair._1, sp.s())) {
                                finalOutputs.add(pair);
                                break;
                            }
                        }
                    }
                }
                return finalOutputs;
            }
        } else {
            if (AdaptiveMap.class.isAssignableFrom(k.componentType)) {
                return k.outputs;
            }
            List<S.Pair> pairs = new ArrayList<>(k.outputs.size());
            for (S.Pair pair : k.outputs) {
                if (null != pair._2) {
                    pairs.add(pair);
                } else {
                    for (S.Pair pair0 : allFields) {
                        if (S.eq(pair._1, pair0._1)) {
                            pairs.add(pair0);
                            break;
                        }
                    }
                }
            }
            return pairs;
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

    private boolean hasPattern2(Collection<S.Pair> ls, List<StringOrPattern> lsp) {
        boolean b = false;
        for (S.Pair pair: ls) {
            StringOrPattern sp = new StringOrPattern(pair._1);
            b = b || sp.isPattern();
            lsp.add(sp);
        }
        return b;
    }


}
