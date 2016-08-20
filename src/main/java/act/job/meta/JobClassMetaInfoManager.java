package act.job.meta;

import act.app.App;
import act.asm.Type;
import act.util.AsmTypes;
import act.util.DestroyableBase;
import org.osgl.util.C;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

import static act.Destroyable.Util.destroyAll;

@ApplicationScoped
public class JobClassMetaInfoManager extends DestroyableBase {

    private Map<String, JobClassMetaInfo> jobs = C.newMap();
    private Map<Type, List<JobClassMetaInfo>> subTypeInfo = C.newMap();

    public JobClassMetaInfoManager() {
    }

    @Override
    protected void releaseResources() {
        destroyAll(jobs.values(), ApplicationScoped.class);
        jobs.clear();
        for (List<JobClassMetaInfo> l : subTypeInfo.values()) {
            destroyAll(l);
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
                        subTypes = C.newList();
                    }
                    subTypes.add(metaInfo);
                }
            }
        }
        List<JobClassMetaInfo> subTypes = subTypeInfo.get(metaInfo.type());
        if (null != subTypes) {
            subTypeInfo.remove(metaInfo.type());
        }
        App.logger.trace("Job meta info registered for: %s", className);
    }

    public JobClassMetaInfo jobMetaInfo(String className) {
        return jobs.get(className);
    }


}
