package act.xio;

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

/**
 * An `NetworkDispatcher` can dispatch a network computation context to a worker thread
 */
public interface NetworkDispatcher {

    /**
     * Dispatch handling request job (to the worker thread)
     * @param job the request handling job
     */
    void dispatch(NetworkJob job);

    /**
     * Keep the state of the network request/response so we can come back
     * to it later on.
     *
     * This method is mainly used when app is running in dev mode and there
     * are code changes triggered app refresh, we need to wait for until the
     * refreshed app started
     */
    void keep();

}
