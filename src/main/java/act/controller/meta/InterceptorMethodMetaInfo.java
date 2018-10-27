package act.controller.meta;

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

import act.Constants;
import act.handler.builtin.controller.Handler;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Set;

/**
 * Stores all method level information needed to generate
 * {@link Handler interceptors}
 */
public class InterceptorMethodMetaInfo extends HandlerMethodMetaInfo<InterceptorMethodMetaInfo> {

    private Set<String> whiteList = C.newSet();
    private Set<String> blackList = C.newSet();
    private Integer priority;

    protected InterceptorMethodMetaInfo(InterceptorMethodMetaInfo copy, ControllerClassMetaInfo clsInfo) {
        super(copy, clsInfo);
        this.whiteList = copy.whiteList;
        this.blackList = copy.blackList;
        this.priority = copy.priority;
    }

    public InterceptorMethodMetaInfo(ControllerClassMetaInfo clsInfo) {
        super(clsInfo);
    }

    @Override
    protected void releaseResources() {
        whiteList.clear();
        blackList.clear();
        super.releaseResources();
    }

    public InterceptorMethodMetaInfo addOnly(String... only) {
        if (!blackList.isEmpty()) {
            warn("Both [only] and [except] list are used for interceptor method[%s]. You should use only one", name());
        }
        return addTo(whiteList, only);
    }

    public InterceptorMethodMetaInfo addExcept(String... except) {
        if (!whiteList.isEmpty()) {
            warn("Both [only] and [except] list are used for interceptor method[%s]. You should use only one", name());
            // when white list is used, black list is ignored
            return this;
        }
        return addTo(blackList, except);
    }

    public InterceptorMethodMetaInfo priority(int priority) {
        this.priority = priority;
        return this;
    }

    public Integer priority() {
        return priority;
    }

    public Set<String> whiteList() {
        return C.Set(whiteList);
    }

    public Set<String> blackList() {
        return C.Set(blackList);
    }

    void mergeInto(C.List<InterceptorMethodMetaInfo> list, String targetName) {
        if (whiteList.contains(targetName) || !blackList.contains(targetName)) {
            if (!list.contains(this)) {
                list.add(this);
            }
        }
    }

    private InterceptorMethodMetaInfo addTo(Set<String> set, String... strings) {
        int len = strings.length;
        if (len == 0) {
            return this;
        }
        for (int i = 0; i < len; ++i) {
            String[] sa = strings[i].split(Constants.LIST_SEPARATOR);
            int saLen = sa.length;
            for (int j = 0; j < saLen; ++j) {
                set.add(sa[j]);
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return toStrBuffer(S.newBuffer()).toString();
    }

    @Override
    protected S.Buffer toStrBuffer(S.Buffer sb) {
        S.Buffer prependix = S.newBuffer();
        if (null != priority) {
            prependix.append("p[")
                    .append(priority).append("] ");
        }
        if (!whiteList.isEmpty()) {
            prependix.append("+").append(whiteList).append(" ");
        }
        if (!blackList.isEmpty()) {
            prependix.append("-").append(blackList).append(" ");
        }

        return super.toStrBuffer(sb).prepend(prependix);
    }

    public final InterceptorMethodMetaInfo extended(ControllerClassMetaInfo clsInfo) {
        if (!this.isStatic() && !clsInfo.isAbstract() && clsInfo.isMyAncestor(this.classInfo())) {
            return doExtend(clsInfo);
        } else {
            return this;
        }
    }

    protected InterceptorMethodMetaInfo doExtend(ControllerClassMetaInfo clsInfo) {
        return new InterceptorMethodMetaInfo(this, clsInfo);
    }
}
