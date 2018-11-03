package act.cli.meta;

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

import act.cli.builtin.Help;
import org.osgl.util.E;
import org.osgl.util.S;

/**
 * Store the option annotation meta info. There are two mutually exclusive
 * option annotations:
 * <ul>
 *     <li>{@link act.cli.Optional}</li>
 *     <li>{@link act.cli.Required}</li>
 * </ul>
 */
public class OptionAnnoInfoBase {
    /*
     * e.g -o
     */
    private String lead1;
    /*
     * e.g. --option
     */
    private String lead2;
    private String defVal;
    private String group;
    private String help = "";
    private boolean required;
    private String param;

    public OptionAnnoInfoBase(boolean optional) {
        this.required = !optional;
    }

    @Override
    public String toString() {
        return S.fmt("%s %s", leads(), help());
    }

    public OptionAnnoInfoBase spec(String[] specs) {
        E.illegalArgumentIf(null == specs || specs.length == 0);
        lead1 = specs[0];
        if (specs.length > 1) {
            lead2 = specs[1];
        } else {
            String[] sa = lead1.split("[,;\\s]+");
            if (sa.length > 2) {
                throw E.unexpected("Option cannot have more than two leads");
            }
            if (sa.length > 1) {
                lead1 = sa[0];
                lead2 = sa[1];
            }
        }
        Help.updateMaxWidth(leads().length());
        return this;
    }

    public String lead1() {
        return lead1;
    }

    public String lead2() {
        return lead2;
    }

    public String leads() {
        if (null == lead1 && null == lead2) {
            return "";
        }
        if (null == lead1) {
            return lead2;
        } else if (null == lead2) {
            return lead1;
        }
        return S.join(",", lead1, lead2);
    }

    private void setLeadsIfNotSet(String paramName) {
        if (null == lead1) {
            lead1 = "-" + paramName.charAt(0);
            lead2 = "--" + paramName;
        }
    }

    public OptionAnnoInfoBase required(boolean required) {
        this.required = required;
        return this;
    }

    public boolean required() {
        return required;
    }

    public OptionAnnoInfoBase defVal(String defVal) {
        this.defVal = defVal;
        return this;
    }

    public String defVal() {
        return defVal;
    }

    public OptionAnnoInfoBase group(String group) {
        this.group = group;
        return this;
    }

    public String group() {
        return group;
    }

    public OptionAnnoInfoBase paramName(String name) {
        param = name;
        setLeadsIfNotSet(name);
        return this;
    }

    public OptionAnnoInfoBase help(String helpMessage) {
        help = helpMessage;
        return this;
    }

    public String help() {
        return help;
    }
}
