package act.util;

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

import act.data.annotation.Data;
import act.inject.param.NoBind;
import com.alibaba.fastjson.JSONObject;
import org.osgl.Lang;
import org.osgl.util.BeanInfo;

import java.util.Map;
import java.util.Set;

@Data
public class AdaptiveBean extends AdaptiveBeanBase<AdaptiveBean> {

    @NoBind
    private transient JSONObject kv = new JSONObject(true);

    private transient volatile EnhancedAdaptiveMap.MetaInfo metaInfo;

    @Override
    public Map<String, Object> internalMap() {
        return kv;
    }

    // --- implement KV
    @Override
    public AdaptiveBean putValue(String key, Object val) {
        Util.putValue(this, key, val);
        return this;
    }

    @Override
    public AdaptiveBean mergeValue(String key, Object val) {
        Util.mergeValue(this, key, val);
        return this;
    }

    @Override
    public <T> T getValue(String key) {
        return Util.getValue(this, key);
    }

    @Override
    public AdaptiveBean putValues(Map<String, Object> map) {
        Util.putValues(this, map);
        return this;
    }

    @Override
    public AdaptiveBean mergeValues(Map<String, Object> map) {
        Util.mergeValues(this, map);
        return this;
    }

    @Override
    public boolean containsKey(String key) {
        return Util.containsKey(this, key);
    }

    @Override
    public Map<String, Object> toMap() {
        return Util.toMap(this);
    }

    @Override
    public int size() {
        return Util.size(this);
    }

    @Override
    public Set<String> keySet() {
        return Util.keySet(this);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return entrySet(null);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet(Lang.Function<BeanInfo, Boolean> fieldFilter) {
        return Util.entrySet(this, fieldFilter);
    }

    public Map<String, Object> asMap() {
        return Util.asMap(this);
    }

    @Override
    @java.beans.Transient
    public EnhancedAdaptiveMap.MetaInfo metaInfo() {
        if (null == metaInfo) {
            synchronized (this) {
                if (null == metaInfo) {
                    metaInfo = Util.generateMetaInfo(this);
                }
            }
        }
        return metaInfo;
    }

}
