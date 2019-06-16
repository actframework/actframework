package act.validation;

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

import act.Act;
import act.util.TopLevelDomainList;
import org.osgl.util.S;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EmailHandler implements ConstraintValidator<Email, CharSequence> {

    private TopLevelDomainList tldList;

    @Override
    public void initialize(Email email) {
        tldList = Act.getInstance(TopLevelDomainList.class);
    }

    @Override
    public boolean isValid(CharSequence charSequence, ConstraintValidatorContext constraintValidatorContext) {
        return isValid(charSequence);
    }

    private boolean isValid(Object val) {
        String s = S.string(val);
        boolean valid = (S.isBlank(s) || s.toLowerCase().matches("^[_a-z0-9-']+(\\.[_a-z0-9-']+)*(\\+[0-9]+)?@[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,64})$"));
        if (!valid) {
            return false;
        }
        // check tld
        String tld = S.cut(s).afterLast(".");
        return tldList.isTld(tld);
    }
}
