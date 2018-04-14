package act.app;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// So static map fields can be cleared after app restart
class ManagedCollectionService extends AppServiceBase<ManagedCollectionService> {

    List<Map> mapList = new ArrayList<>();

    ManagedCollectionService(App app) {
        super(app);
    }

    @Override
    protected void releaseResources() {
        for (Map map : mapList) {
            map.clear();
        }
        mapList.clear();
    }

    <K, V> Map<K, V> createMap() {
        Map<K, V> map = new HashMap<>();
        mapList.add(map);
        return map;
    }

    <K, V> ConcurrentMap<K, V> createConcurrentMap() {
        ConcurrentMap<K, V> map = new ConcurrentHashMap<>();
        mapList.add(map);
        return map;
    }

    <E> Set<E> createSet() {
        return new HashSet<>();
    }



}
