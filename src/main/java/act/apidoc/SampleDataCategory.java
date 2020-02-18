package act.apidoc;

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

import org.osgl.util.C;
import org.osgl.util.Keyword;
import org.osgl.util.S;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum SampleDataCategory implements ISampleDataCategory {
    ID("id", "no"),
    FIRST_NAME("given name", "forename", "firstname", "fname"),
    LAST_NAME("surname", "family name", "lname", "lastname"),
    FULL_NAME("fullname"),
    USERNAME("userName", "login", "loginName"),
    DOB("Birthday", "生日", "DateOfBirth", "*date", "date*"),
    EMAIL("mail"),
    URL("uri"),
    HOST,
    STREET,
    SUBURB("city"),
    POSTCODE("postalCode", "postalcode", "post", "postCode"),
    PHONE("landphone", "landline", "landLine", "landPhone", "tel", "phoneNumber"),
    MOBILE("mobilePhone", "cellularPhone", "mobileNo", "mobileNumber"),
    STATE("province"),
    COMPANY_NAME("clientName", "client", "company", "organizationName", "organisationName", "organization", "organisation"),
    PASSWORD,
    PERMISSION,
    PERMISSIONS,
    ROLE("roleName"),
    ROLES("roleNames"),
    PRIVILEGE,
    ;

    private Set<String> aliases = C.Set();

    SampleDataCategory() {}

    SampleDataCategory(String ... aliases) {
        this.aliases = C.setOf(aliases);
    }

    @Override
    public Set<String> aliases() {
        return aliases;
    }

    private static Map<Keyword, SampleDataCategory> lookup = new HashMap<>();
    private static Map<String, SampleDataCategory> prefixLookup = new HashMap<>();
    private static Map<String, SampleDataCategory> suffixLookup = new HashMap<>();
    static {
        for (SampleDataCategory c : SampleDataCategory.values()) {
            lookup.put(Keyword.of(c.name()), c);
            for (String s : c.aliases) {
                if (s.startsWith("*")) {
                    suffixLookup.put(s.substring(1).toLowerCase(), c);
                } else if (s.endsWith("*")) {
                    prefixLookup.put(s.substring(0, s.length() - 1).toLowerCase(), c);
                } else {
                    lookup.put(Keyword.of(s), c);
                }
            }
        }
    }

    public static SampleDataCategory of(String s) {
        if (S.blank(s)) {
            return null;
        }
        SampleDataCategory category = lookup.get(Keyword.of(s));
        if (null != category) {
            return category;
        }
        s = s.toLowerCase();
        for (Map.Entry<String, SampleDataCategory> entry : prefixLookup.entrySet()) {
            if (s.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        for (Map.Entry<String, SampleDataCategory> entry : suffixLookup.entrySet()) {
            if (s.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return S.blank(s) ? null : lookup.get(Keyword.of(s));
    }
}
