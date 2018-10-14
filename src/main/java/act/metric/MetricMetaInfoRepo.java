package act.metric;

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

import act.app.App;
import act.app.AppServiceBase;
import act.app.event.SysEventId;
import act.util.ClassInfoRepository;
import act.util.ClassNode;
import org.osgl.util.S;

import java.util.*;

public class MetricMetaInfoRepo extends AppServiceBase<MetricMetaInfoRepo> {

    /**
     * Map class name to metric context
     */
    private Map<String, String> contexts = new HashMap<>();

    public MetricMetaInfoRepo(final App app) {
        super(app);
        app.jobManager().on(SysEventId.APP_CODE_SCANNED, "MetricMetaInfoRepo:mergeFromParents", new Runnable() {
            @Override
            public void run() {
                mergeFromParents(app.classLoader().classInfoRepository());
            }
        });
    }

    @Override
    protected void releaseResources() {
        contexts.clear();
    }

    public void registerMetricContext(String className, String context) {
        contexts.put(className, context);
    }

    public String contextOfClass(String className) {
        return contexts.get(className);
    }

    private void mergeFromParents(ClassInfoRepository classInfoRepository) {
        // sort class names so that super class always be processed before extended class
        SortedSet<String> sortedClassNames = new TreeSet<>(classInfoRepository.parentClassFirst);
        sortedClassNames.addAll(contexts.keySet());
        for (String className: sortedClassNames) {
            String context = contexts.get(className);
            if (context.startsWith("/")) {
                context = calibrate(context);
            } else {
                ClassNode node = classInfoRepository.node(className);
                if (null != node) {
                    ClassNode parentNode = node.parent();
                    if (null != parentNode) {
                        String parentContext = contexts.get(parentNode.name());
                        context = concat(parentContext, context);
                    }
                }
            }
            contexts.put(className, context);
        }
    }

    // ensure `/` be converted to `:`
    private static String calibrate(String context) {
        while(context.startsWith("/")) context = context.substring(1);
        return context.replace('/', ':');
    }

    public static String concat(String parent, String child) {
        return child.startsWith("/") || S.blank(parent) ? calibrate(child) : calibrate(S.pathConcat(parent, ':', child));
    }

}
