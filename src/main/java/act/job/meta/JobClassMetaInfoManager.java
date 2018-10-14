package act.job.meta;

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

import static act.Destroyable.Util.destroyAll;

import act.asm.Type;
import act.util.AsmTypes;
import act.util.LogSupportedDestroyableBase;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JobClassMetaInfoManager extends LogSupportedDestroyableBase {

    private Map<String, JobClassMetaInfo> jobs = new HashMap<>();
    private Map<Type, List<JobClassMetaInfo>> subTypeInfo = new HashMap<>();

    public JobClassMetaInfoManager() {
    }

    @Override
    protected void releaseResources() {
        destroyAll(jobs.values(), ApplicationScoped.class);
        jobs.clear();
        for (List<JobClassMetaInfo> l : subTypeInfo.values()) {
            destroyAll(l, ApplicationScoped.class);
        }
        subTypeInfo.clear();
        super.releaseResources();
    }

    public void registerJobMetaInfo(JobClassMetaInfo metaInfo) {
        String className = Type.getObjectType(metaInfo.className()).getClassName();
        jobs.put(className, metaInfo);
        if (metaInfo.isJob()) {
            Type superType = metaInfo.superType();
            if (!AsmTypes.OBJECT_TYPE.equals(superType)) {
                JobClassMetaInfo superInfo = jobMetaInfo(superType.getClassName());
                if (null == superInfo) {
                    List<JobClassMetaInfo> subTypes = subTypeInfo.get(superType);
                    if (null == subTypes) {
                        subTypes = new ArrayList<>();
                    }
                    subTypes.add(metaInfo);
                }
            }
        }
        List<JobClassMetaInfo> subTypes = subTypeInfo.get(metaInfo.type());
        if (null != subTypes) {
            subTypeInfo.remove(metaInfo.type());
        }
        trace("Job meta info registered for: %s", className);
    }

    public JobClassMetaInfo jobMetaInfo(String className) {
        return jobs.get(className);
    }


}
