package act.job;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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

import act.inject.util.Sorter;
import act.util.LogSupport;
import act.util.LogSupportedDestroyableBase;
import org.osgl.util.C;

import java.util.*;

public class JobExceptionListenerManager extends LogSupportedDestroyableBase {

    private Map<String, List<JobExceptionListener>> repo = new HashMap<>();
    private List<JobExceptionListener> globalListeners = new ArrayList<>();

    @Override
    protected void releaseResources() {
        trace("release JobExceptionListenerManager");
        repo.clear();
        globalListeners.clear();
    }

    public void registerJobExceptionListener(JobExceptionListener l) {
        String[] jobIds = l.listenTo();
        if (null == jobIds || jobIds.length == 0) {
            globalListeners.add(l);
            Collections.sort(globalListeners, Sorter.COMPARATOR);
            return;
        }
        Set<String> set = C.setOf(jobIds);
        for (String jobId : set) {
            List<JobExceptionListener> list = repo.get(jobId);
            if (null == list) {
                list = new ArrayList<>();
                repo.put(jobId, list);
            }
            list.add(l);
        }
    }

    public void handleJobException(String jobId, Exception e) {
        List<JobExceptionListener> listeners = repo.get(jobId);
        List<JobExceptionListener> allListeners;
        if (null != listeners) {
            allListeners = new ArrayList<>();
            allListeners.addAll(listeners);
            allListeners.addAll(globalListeners);
            Collections.sort(allListeners, Sorter.COMPARATOR);
        } else {
            allListeners = globalListeners;
        }
        for (JobExceptionListener l : allListeners) {
            try {
                if (l.handleJobException(jobId, e)) {
                    return;
                }
            } catch (Exception e2) {
                warn(e, "Job exception encountered on running job[%s]", jobId);
                error(e, "Error calling JobExceptionListener[%s] on Exception[%s]", l, e);
                return;
            }
        }
        warn(e, "Job exception encountered on running job[%s]", jobId);
    }

}
