package act.util;

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

import act.app.ActionContext;
import act.cli.CliContext;
import act.cli.CliSession;
import act.controller.meta.HandlerMethodMetaInfo;
import org.osgl.$;
import org.osgl.Lang;
import org.osgl.util.C;
import org.osgl.util.S;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
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
 *     {@literal @}PropertySpec({"firstName","lastName","email"})
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
 * <p>
 *     When the result is to be presented on a {@link CliSession} and
 *     {@code PropertySpec} annotation is presented, either {@link act.cli.TableView}
 *     or {@link act.util.JsonView} can be used to define the presenting style.
 *     If both {@code TableView} and {@code JsonView} are found on the method
 *     then {@code JsonView} is the winner. If non of them is presented then
 *     {@code JsonView} will be used by default
 * </p>
 * <p>
 *     When the result is to be write to an {@link org.osgl.http.H.Response}, and
 *     {@code PropertySpec} annotation is presented on the controller action method,
 *     then the return value (if not of type {@link org.osgl.mvc.result.Result}) will
 *     be serialized into a JSON string and the filter will effect and impact the
 *     JSON string
 * </p>
 * @see act.cli.TableView
 * @see act.util.JsonView
 * @see FastJsonPropertyPreFilter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PropertySpec {
    /**
     * Specify the object fields to be displayed in final result. E.g.
     * <pre>
     * {@literal @}PropertySpec({"firstName","lastName","email"})
     * </pre>
     * You can specify multiple fields in one string, with fields
     * separated with one of the following character: {@code ,;|}
     * <pre>
     * {@literal @}PropertySpec("firstName,lastName,email")
     * </pre>
     * You can use {@code as} to specify the label, e.g.
     * <pre>
     * {@literal @}PropertySpec("fn as First name,ln as Last name,email as Email")
     * </pre>
     * If there are multiple levels of objects, use {@code .} or {@code /} to
     * express the traverse path:
     * <pre>
     * {@literal @}PropertySpec("fn,ln,contact.address.street,contact.address.city,contact.email")
     * </pre>
     * Instead of specifying fields to be exported, it can also specify the fields to be
     * excluded from output with the symbol {@code -}, e.g.
     * <pre>
     * {@literal @}PropertySpec("-password,-salary")
     * </pre>
     * when symbol {@code -} is used to specify the excluded fields, then all the fields
     * without symbol {@code -} in the list will be ignored. However it can still use
     * {@code as} notation to specify the label. E.g.
     * <pre>
     * {@literal @}PropertySpec("-password,-salary,fn as firstName,ln as lastName")
     * </pre>
     * @return the field specification
     */
    String[] value() default {};

    /**
     * Specify the spec for command line interface output
     * <p>
     *     If not specified, then it will use the spec specified in {@link #value()}
     *     when output to CLI
     * </p>
     * @return the field specification for CLI
     * @see #value()
     */
    String[] cli() default {};

    /**
     * Specify the spec for http response output
     * <p>
     *     If not specified, then it will use the spec specified in {@link #value()}
     *     when output to http response
     * </p>
     * @return the field specification for http
     * @see #value()
     */
    String[] http() default {};

    /**
     * Capture the {@code PropertySpec} annotation meta info in bytecode scanning phase
     */
    class MetaInfo {
        // split "fn as firstName" into "fn" and "firstName"
        private static Pattern p = Pattern.compile("\\s+as\\s+", Pattern.CASE_INSENSITIVE);

        public static class Spec extends $.T3<List<String>, Set<String>, Map<String, String>> {

            Spec() {
                super(C.<String>newList(), C.<String>newSet(), C.<String, String>newMap());
            }

            List<String> outputs() {
                return _1;
            }

            Set<String> excluded() {
                return _2;
            }

            Map<String, String> labels() {
                return _3;
            }

            boolean isEmpty() {
                return _1.isEmpty() && _2.isEmpty() && _3.isEmpty();
            }

            public Lang._MappingStage applyTo(Lang._MappingStage stage) {
                if (!outputs().isEmpty()) {
                    stage.filter(S.join(",", outputs()));
                } else if (!excluded().isEmpty()) {
                    stage.filter(S.join(",", C.list(excluded()).map(S.F.prepend("-"))));
                }
                if (!labels().isEmpty()) {
                    stage.withHeadMapping(labels());
                }
                return stage;
            }

        }

        private static Spec newSpec() {
            return new Spec();
        }

        private Spec common = newSpec();
        private Spec cli = newSpec();
        private Spec http = newSpec();

        private String commonRaw;
        private String cliRaw;
        private String httpRaw;

        public void onValue(String value) {
            commonRaw = value;
            _on(value, common);
        }

        public void onCli(String value) {
            cliRaw = value;
            _on(value, cli);
        }

        public void onHttp(String value) {
            httpRaw = value;
            _on(value, http);
        }

        public void ensureValid() {
            if (common.isEmpty() && http.isEmpty() && cli.isEmpty()) {
                throw new IllegalStateException("no spec defined");
            }
        }

        private void _on(String string, Spec spec) {
            String[] sa = string.split("[,;]+");
            for (String s: sa) {
                s = s.trim();
                if (s.startsWith("-")) {
                    spec.excluded().add(s.substring(1));
                    spec.outputs().clear();
                } else {
                    String[] sa0 = p.split(s);
                    if (sa0.length > 1) {
                        String k = sa0[0].trim(), v = sa0[1].trim();
                        spec.labels().put(k, v);
                        if (spec.excluded().isEmpty()) {
                            spec.outputs().add(k);
                        }
                    } else if (spec.excluded().isEmpty()) {
                        spec.outputs().add(s.trim());
                    }
                }
            }
        }

        @Deprecated
        public List<String> outputFields() {
            return C.list(common.outputs());
        }

        public String raw(ActContext context) {
            if (null == context) {
                return commonRaw;
            } else if (context instanceof ActionContext) {
                return S.blank(httpRaw) ? commonRaw : httpRaw;
            } else if (context instanceof CliContext) {
                return S.blank(cliRaw) ? commonRaw : cliRaw;
            } else {
                // mail context is unlikely to happen
                throw new IllegalStateException("context not applied: " + context);
            }
        }

        public List<String> outputFields(ActContext context) {
            Spec spec = spec(context);
            return null == spec ? C.<String>list() : spec.outputs();
        }

        public List<S.Pair> outputFieldsAndLabel(ActContext context) {
            Spec spec = spec(context);
            if (null == spec) {
                return C.list();
            }
            List<String> outputs = spec.outputs();
            List<S.Pair> pairs = new ArrayList<>();
            for (String f : outputs) {
                S.Pair pair = S.pair(f, spec.labels().get(f));
                pairs.add(pair);
            }
            return pairs;
        }

        public List<String> outputFieldsForHttp() {
            Spec spec = httpSpec();
            return null == spec ? C.<String>list() : spec.outputs();
        }

        public List<String> labels(List<String> outputs, ActContext context) {
            List<String> retList = new ArrayList<>();
            for (String f : outputs) {
                retList.add(label(f, context));
            }
            return retList;
        }

        public List<String> labels2(List<S.Pair> outputs, ActContext context) {
            List<String> retList = new ArrayList<>();
            for (S.Pair pair : outputs) {
                retList.add(label(pair, context));
            }
            return retList;
        }

        public Map<String, String> labelMapping() {
            return C.Map(common.labels());
        }

        public Map<String, String> labelMapping(ActContext context) {
            return null == context ? labelMapping() : C.Map(spec(context).labels());
        }

        public Set<String> excludedFields(ActContext context) {
            Spec spec = spec(context);
            return null == spec ? C.<String>Set() : C.Set(spec.excluded());
        }

        public Set<String> excludeFieldsForHttp() {
            Spec spec = httpSpec();
            return null == spec ? C.<String>Set() : C.Set(spec.excluded());
        }

        public String label(String field, ActContext context) {
            String lbl = spec(context).labels().get(field);
            return null == lbl ? field : lbl;
        }

        public String label(S.Pair field, ActContext context) {
            String lbl = spec(context).labels().get(field._1);
            String defLabel = field._2;
            if (null == defLabel) {
                defLabel = field._1;
            }
            return null == lbl ? defLabel : lbl;
        }

        public void setLabelIfNotFound(String field, String label, ActContext context) {
            Map<String, String> map = spec(context).labels();
            if (!map.containsKey(field)) {
                map.put(field, label);
            }
        }

        public Lang._MappingStage applyTo(Lang._MappingStage mappingStage, ActContext context) {
            return spec(context).applyTo(mappingStage);
        }

        private Spec httpSpec() {
            return null == http || http.isEmpty() ? common : http;
        }

        private Spec spec(ActContext context) {
            if (null == context) {
                return common;
            }
            if (context instanceof ActionContext) {
                return null == http || http.isEmpty() ? common : http;
            } else if (context instanceof CliContext) {
                return null == cli || cli.isEmpty() ? common : cli;
            } else {
                // mail context is unlikely to happen
                throw new IllegalStateException("context not applied: " + context);
            }
        }

        public static MetaInfo withCurrentNoConsume(MetaInfo builtIn, ActContext context) {
            return _withCurrent(builtIn, context, false);
        }

        public static MetaInfo withCurrent(MetaInfo builtIn, ActContext context) {
            return _withCurrent(builtIn, context, true);
        }

        private static MetaInfo _withCurrent(MetaInfo builtIn, ActContext context, boolean consume) {
            // see https://github.com/actframework/actframework/issues/1118
            if (consume && null != context) {
                if (context.isPropertySpecConsumed()) {
                    return null;
                }
                context.markPropertySpecConsumed();
            }
            MetaInfo retVal = builtIn;
            MetaInfo spec = currentSpec.get();
            if (null != spec) {
                retVal = spec;
            } else {
                String s = PropertySpec.current.get();
                if (S.notBlank(s)) {
                    spec = new PropertySpec.MetaInfo();
                    if (context instanceof CliContext) {
                        spec.onCli(s);
                    } else {
                        spec.onHttp(s);
                    }
                    retVal = spec;
                }
            }
            if (context instanceof ActionContext) {
                ActionContext actionContext = (ActionContext) context;
                actionContext.propertySpec(retVal);
            }
            return retVal;
        }

        public static MetaInfo withCurrent(HandlerMethodMetaInfo methodMetaInfo, ActContext context) {
            MetaInfo builtIn = null == methodMetaInfo ? null : methodMetaInfo.propertySpec();
            return withCurrent(builtIn, context);
        }
    }

    ThreadLocal<String> current = new ThreadLocal<>();
    ThreadLocal<PropertySpec.MetaInfo> currentSpec = new ThreadLocal<>();

}
