package act.job.meta;

import act.asm.Type;
import act.job.*;
import act.util.DestroyableBase;
import org.osgl.util.C;
import org.osgl.util.S;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;

import static act.Destroyable.Util.destroyAll;

/**
 * Stores all class level information to support generating of
 * Job worker class that wrap the annotated Job action method
 */
@ApplicationScoped
public final class JobClassMetaInfo extends DestroyableBase {

    private Type type;
    private Type superType;
    private boolean isAbstract = false;
    private C.List<JobMethodMetaInfo> actions = C.newList();
    // actionLookup index job method by method name
    private C.Map<String, JobMethodMetaInfo> actionLookup = null;
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
        C.Map<String, JobMethodMetaInfo> lookup = C.newMap();
        for (JobMethodMetaInfo act : actions) {
            lookup.put(act.name(), act);
        }
        actionLookup = lookup;
    }

    private static final C.Set<Class<? extends Annotation>> ACTION_ANNOTATION_TYPES = C.set(
            AlongWith.class, Cron.class, Every.class,
            FixedDelay.class, InvokeAfter.class, InvokeBefore.class,
            OnAppStart.class, OnAppStop.class, OnAppEvent.class);

    public static boolean isActionAnnotation(Class<? extends Annotation> type) {
        return ACTION_ANNOTATION_TYPES.contains(type);
    }

}
