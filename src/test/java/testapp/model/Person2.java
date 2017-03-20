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
import act.util.EqualIgnore;
import org.osgl.util.S;

@Data
public class Person2 {

    public enum Gender {
        M, F
    }

    @EqualIgnore
    private String v1;
    private String firstName;
    private String lastName;
    private Address2 address;
    private Gender gender = Gender.M;
    private int age;
    private int height;

    public Person2(String fn, String ln, Address2 addr, Integer age, Gender gender) {
        firstName = fn;
        lastName = ln;
        address = addr;
        this.age = age;
        this.gender = gender;
        this.height = 178;
        v1 = S.random();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Address2 getAddress() {
        return address;
    }

    public void setAddress(Address2 address) {
        this.address = address;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void happyBirthday() {
        age++;
    }

}
