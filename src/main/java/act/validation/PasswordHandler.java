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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PasswordHandler implements ConstraintValidator<Password, char[]> {

    private Password.Validator validator;

    @Override
    public void initialize(final Password password) {
        final String pattern = password.value();
        if (Password.DEFAULT_PATTERN.equals(pattern)) {
            validator = Act.appConfig().defPasswordValidator();
        } else {
            try {
                validator = PasswordSpec.parse(pattern);
            } catch (Exception e0) {
                try {
                    validator = Act.getInstance(pattern);
                } catch (Exception e1) {
                    throw new IllegalArgumentException("Unknown password spec: " + pattern);
                }
            }
        }
    }

    @Override
    public boolean isValid(char[] password, ConstraintValidatorContext constraintValidatorContext) {
        return validator.isValid(password);
    }
}
