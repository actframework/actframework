package act.util;

import org.osgl.$;
import org.osgl.util.C;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Mark on a method (could be cli command, or controller action)
 * to specify the fields to be exported.
 * <p>This annotation is only effective when there is one and only one
 * type of object returned, as a single instance or a collection of instances, e.g</p>
 * <pre>
 *     {@literal @}DataView({"firstName","lastName","email"})
 *     public List&lt;Employee&gt; getEmployees(String search) {
 *         List&lt;Employee&gt; retList = EmployeeDao.find(search).asList();
 *         return retList;
 *     }
 * </pre>
 * Suppose the request accept {@code application/json} type, then only the following
 * field of the {@code Employee} instances will be exported in JSON output:
 * <ul>
 *     <li>firstName</li>
 *     <li>lastName</li>
 *     <li>email</li>
 * </ul>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface DataView {
    /**
     * Specify the object fields to be displayed in final result. E.g.
     * <pre>
     * {@literal @}DataView({"firstName","lastName","email"})
     * </pre>
     * You can specify multiple fields in one string, with fields
     * separated with one of the following character: {@code ,;:|}
     * <pre>
     * {@literal @}DataView("firstName,lastName,email")
     * </pre>
     * You can use {@code as} to specify the label, e.g.
     * <pre>
     * {@literal @}DataView("fn as First name,ln as Last name,email as Email")
     * </pre>
     * If there are multiple levels of objects, use {@code .} or {@code /} to
     * express the traverse path:
     * <pre>
     * {@literal @}DataView("fn,ln,contact.address.street,contact.address.city,contact.email")
     * </pre>
     * Instead of specifying fields to be exported, it can also specify the fields to be
     * excluded from output with the symbol {@code -}, e.g.
     * <pre>
     * {@literal @}DataView("-password,-salary")
     * </pre>
     * when symbol {@code -} is used to specify the excluded fields, then all the fields
     * without symbol {@code -} in the list will be ignored. However it can still use
     * {@code as} notation to specify the label. E.g.
     * <pre>
     * {@literal @}DataView("-password,-salary,fn as firstName,ln as lastName")
     * </pre>
     * @return the field specification
     */
    String[] value();

    /**
     * Capture the {@code DataView} annotation meta info in bytecode scanning phase
     */
    public static class MetaInfo {
        // split "fn as firstName" into "fn" and "firstName"
        private static Pattern p = Pattern.compile("\\s+as\\s+", Pattern.CASE_INSENSITIVE);
        private List<String> outputs = C.newList();
        private Set<String> excluded = C.newSet();
        private Map<String, String> labels = C.newMap();

        public void onValue(String value) {
            String[] sa = value.split("[,;:]+");
            for (String s: sa) {
                if (s.startsWith("-")) {
                    excluded.add(s);
                    outputs.clear();
                } else {
                    String[] sa0 = p.split(s);
                    if (sa0.length > 1) {
                        String k = sa0[0], v = sa0[1];
                        labels.put(k, v);
                        if (excluded.isEmpty()) {
                            outputs.add(k);
                        }
                    } else if (excluded.isEmpty()) {
                        outputs.add(s);
                    }
                }
            }
        }

        public List<String> outputFields() {
            return C.list(outputs);
        }

        public List<String> labels() {
            List<String> retList = C.newList();
            for (String f : outputs) {
                retList.add(label(f));
            }
            return retList;
        }

        public Set<String> excludedFields() {
            return C.set(excluded);
        }

        public String label(String field) {
            String lbl = labels.get(field);
            return null == lbl ? field : lbl;
        }
    }

}
