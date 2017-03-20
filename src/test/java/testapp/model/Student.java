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
import org.osgl.$;

@Data(callSuper = true)
public class Student extends Person {
    private String clazz;
    private String studentId;
    private double score;

    public Student(String fn, String ln, Address addr, int age, String clazz, String studentId, double score) {
        super(fn, ln, addr, age);
        this.clazz = clazz;
        this.studentId = studentId;
        this.score = score;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Student) {
            Student that = (Student) obj;
            return super.equals(that) && $.eq(that.clazz, this.clazz) && $.eq(that.studentId, this.studentId) && $.eq(that.score, this.score);
        }
        return false;
    }

    @Override
    public int hashCode() {
        String foo = "";
        int bar = 4;
        byte z = 2;
        return $.hc(clazz, studentId, score, foo, bar, z, super.hashCode());
    }

}
