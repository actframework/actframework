package act.controller.meta;

import act.Constants;
import act.handler.builtin.controller.Handler;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Set;

import static act.app.App.logger;

/**
 * Stores all method level information needed to generate
 * {@link Handler interceptors}
 */
public class InterceptorMethodMetaInfo extends HandlerMethodMetaInfo<InterceptorMethodMetaInfo>
        implements Comparable<InterceptorMethodMetaInfo> {

    private Set<String> whiteList = C.newSet();
    private Set<String> blackList = C.newSet();
    private int priority;

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
            logger.warn("Both [only] and [except] list are used for interceptor method[%s]. You should use only one", name());
        }
        return addTo(whiteList, only);
    }

    public InterceptorMethodMetaInfo addExcept(String... except) {
        if (!whiteList.isEmpty()) {
            logger.warn("Both [only] and [except] list are used for interceptor method[%s]. You should use only one", name());
            // when white list is used, black list is ignored
            return this;
        }
        return addTo(blackList, except);
    }

    public InterceptorMethodMetaInfo priority(int priority) {
        this.priority = priority;
        return this;
    }

    public int priority() {
        return priority;
    }

    public Set<String> whiteList() {
        return C.set(whiteList);
    }

    public Set<String> blackList() {
        return C.set(blackList);
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
        StringBuilder sb = S.builder();
        if (0 != priority) {
            sb.append("p[")
                    .append(priority).append("] ");
        }
        if (!whiteList.isEmpty()) {
            sb.append("+").append(whiteList).append(" ");
        }
        if (!blackList.isEmpty()) {
            sb.append("-").append(blackList).append(" ");
        }
        sb.append(super.toString());
        return sb.toString();
    }

    @Override
    public int compareTo(InterceptorMethodMetaInfo o) {
        return o.priority - priority;
    }

    public final InterceptorMethodMetaInfo extended(ControllerClassMetaInfo clsInfo) {
        if (clsInfo.isMyAncestor(this.classInfo()) && !this.isStatic()) {
            return doExtend(clsInfo);
        } else {
            return this;
        }
    }

    protected InterceptorMethodMetaInfo doExtend(ControllerClassMetaInfo clsInfo) {
        return new InterceptorMethodMetaInfo(this, clsInfo);
    }
}
