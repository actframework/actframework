package testapp.model;

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

import act.data.annotation.Data;

/**
 * Created by luog on 28/11/15.
 */
@Data(callSuper = false)
public class Teacher2 extends Person2 {
    private String teacherId;
    public Teacher2(
            String fn, String ln, Address2 addr, Integer age,
            Gender gender, String teacherId) {
        super(fn, ln, addr, age, gender);
        this.teacherId = teacherId;
    }

    public String getTeacherId() {
        return teacherId;
    }

}
