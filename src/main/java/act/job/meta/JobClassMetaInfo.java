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
import act.job.*;
import act.test.FixtureLoader;
import act.util.LogSupportedDestroyableBase;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.*;
import javax.enterprise.context.ApplicationScoped;

/**
 * Stores all class level information to support generating of
 * Job worker class that wrap the annotated Job action method
 */
@ApplicationScoped
public final class JobClassMetaInfo extends LogSupportedDestroyableBase {

    private Type type;
    private Type superType;
    private boolean isAbstract = false;
    private List<JobMethodMetaInfo> actions = new ArrayList<>();
    // actionLookup index job method by method name
    private Map<String, JobMethodMetaInfo> actionLookup = null;
    private boolean isJob;

    public JobClassMetaInfo className(String name) {
        this.type = Type.getObjectType(name);
        return this;
    }

    @Override
    protected void releaseResources() {
        destroyAll(actions, ApplicationScoped.class);
        actions.clear();
        destroyAll(actionLookup.values(), ApplicationScoped.class);
        actionLookup.clear();
        super.releaseResources();
    }

    public String className() {
        return type.getClassName();
    }

    public Type type() {
        return type;
    }

    public JobClassMetaInfo superType(Type type) {
        superType = type;
        return this;
    }

    public Type superType() {
        return superType;
    }

    public JobClassMetaInfo setAbstract() {
        isAbstract = true;
        return this;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isJob() {
        return isJob;
    }

    public JobClassMetaInfo isJob(boolean b) {
        isJob = b;
        return this;
    }

    public JobClassMetaInfo addAction(JobMethodMetaInfo info) {
        actions.add(info);
        return this;
    }

    public JobMethodMetaInfo action(String name) {
        if (null == actionLookup) {
            for (JobMethodMetaInfo act : actions) {
                if (S.eq(name, act.name())) {
                    return act;
                }
            }
            return null;
        }
        return actionLookup.get(name);
    }

    private void buildActionLookup() {
        Map<String, JobMethodMetaInfo> lookup = new HashMap<>();
        for (JobMethodMetaInfo act : actions) {
            lookup.put(act.name(), act);
        }
        actionLookup = lookup;
    }

    private static final C.Set<Class<? extends Annotation>> ACTION_ANNOTATION_TYPES = C.set(
            AlongWith.class, Cron.class, Every.class,
            FixedDelay.class, InvokeAfter.class, InvokeBefore.class,
            OnAppStart.class, OnAppStop.class, OnSysEvent.class, FixtureLoader.class);

    public static boolean isActionAnnotation(Class<? extends Annotation> type) {
        return ACTION_ANNOTATION_TYPES.contains(type);
    }

}
