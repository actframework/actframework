package act.job;

import act.util.ActContext;
import org.osgl.http.H;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Communicate context data across threads
 */
public class JobContext {

    private static ThreadLocal<JobContext> current_ = new ThreadLocal<JobContext>();

    private JobContext() {
        ActContext<?> actContext = ActContext.Base.currentContext();
        if (null != actContext) {
            bag_.put("locale", actContext.locale());
        }
    }

    private static Map<String, Object> m() {
        return current_.get().bag_;
    }

    /**
     * Whether JobContext of current thread initialized
     * @return
     */
    public static boolean initialized() {
        return null != current_.get();
    }

    /**
     * Init JobContext of current thread
     */
    static void init() {
        clear();
        current_.set(new JobContext());
    }

    /**
     * Clear JobContext of current thread
     */
    static void clear() {
        JobContext ctxt = current_.get();
        if (null != ctxt) {
            ctxt.bag_.clear();
            current_.remove();
        }
    }

    /**
     * Get value by key from the JobContext of current thread
     * @param key
     * @return
     */
    public static Object get(String key) {
        return m().get(key);
    }

    /**
     * Generic version of getting value by key from the JobContext of current thread
     * @param key
     * @return
     */
    public static <T> T get(String key, Class<T> clz) {
        return (T)m().get(key);
    }

    /**
     * Set value by key to the JobContext of current thread
     * @param key
     * @return
     */
    public static void put(String key, Object val) {
        m().put(key, val);
    }

    /**
     * Remove value by key from the JobContext of current thread
     * @param key
     * @return
     */
    public static void remove(String key) {
        m().remove(key);
    }

    /**
     * Make a copy of JobContext of current thread
     * @return
     */
    static JobContext copy() {
        JobContext ctxt = new JobContext();
        JobContext current = current_.get();
        if (null != current) {
            ctxt.bag_.putAll(current.bag_);
        }
        return ctxt;
    }

    /**
     * Initialize current thread's JobContext using specified copy
     * @param origin
     */
    static void init(JobContext origin) {
        current_.set(origin);
        if (origin.bag_.isEmpty()) {
            return;
        }
        ActContext<?> actContext = ActContext.Base.currentContext();
        if (null != actContext) {
            Locale locale = (Locale) origin.bag_.get("locale");
            if (null != locale) {
                actContext.locale(locale);
            }
        }
    }

    private Map<String, Object> bag_ = new HashMap<>();

}
