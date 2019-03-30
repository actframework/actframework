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

import act.util.AdaptiveBean;
import act.util.LogSupport;

/**
 * App developer can define `JobExceptionListener` to
 * process the case when an exception is raised during
 * a {@link Job} running.
 */
public interface JobExceptionListener {

    /**
     * A special ID list indicates all Jobs.
     */
    String[] ALL_JOB = {};

    /**
     * Returns the id list of jobs that this listener
     * is interested.
     *
     * Only exception happening on the job with id
     * included in the returning list will be routed
     * to the listener.
     *
     * If the listener would like to listen to
     * exception of all jobs, then return an empty
     * array or a `null` array.
     *
     * @return a set of job ids.
     */
    String[] listenTo();

    /**
     * Handle exception `e`
     * @param jobId the id of the job that triggered the exception
     * @param e the exception raised in Job execution
     * @return `true` if the exception is handled and framework shall not
     *         proceed with next job exception listener; `false` otherwise.
     */
    boolean handleJobException(String jobId, Exception e);

    abstract class Adaptor extends LogSupport implements JobExceptionListener {
    }

    abstract class GlobalAdaptor extends Adaptor {
        @Override
        public String[] listenTo() {
            return ALL_JOB;
        }
    }
}
