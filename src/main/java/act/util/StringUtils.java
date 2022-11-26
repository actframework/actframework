package act.util;

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

import act.Act;
import act.app.App;
import com.alibaba.fastjson.JSON;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;
import org.osgl.util.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StringUtils {

    static $.Transformer<String, String> evaluator = new $.Transformer<String, String>() {
        @Override
        public String transform(String s) {
            App app = Act.app();
            return S.string(null != app ? app.config().get(s) : System.getProperty(s));
        }
    };


    public static String processStringSubstitution(String s) {
        return processStringSubstitution(s, evaluator, false);
    }

    public static String processStringSubstitution(String s, boolean ignoreError) {
        return processStringSubstitution(s, evaluator, ignoreError);
    }

    public static String processStringSubstitution(String s, $.Func1<String, String> evaluator) {
        return processStringSubstitution(s, evaluator, false);
    }

    public static String processStringSubstitution(String s, $.Func1<String, String> evaluator, boolean ignoreError) {
        if (S.blank(s)) {
            return "";
        }
        int n = s.indexOf("${");
        if (n < 0) {
            return s;
        }
        int a = 0;
        int z = n;
        StringBuilder buf = S.builder();
        while (true) {
            buf.append(s, a, z);
            n = s.indexOf("}", z);
            a = n + 1;
            String key = s.substring(z + 2, a - 1);
            if (S.notEmpty(key)) {
                String val = key;
                try {
                    val = evaluator.apply(key);
                } catch (RuntimeException e) {
                    if (!ignoreError) throw e;
                    buf.append("${").append(key).append("}");
                }
                buf.append(val);
            } else {
                buf.append("${}");
            }
            n = s.indexOf("${", a);
            if (n < 0) {
                buf.append(s.substring(a));
                return buf.toString();
            }
            z = n;
        }
    }

    public static void main(String[] args) {
        String s = "{\n" +
                "  \"buildNumber\": \"${1151.buildNumber}\",\n" +
                "  \"cliPort\": \"${cli.port}\"\n" +
                "}\n";
        System.setProperty("cli.port", "5461");
        System.out.println(processStringSubstitution(s));
    }


}
