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

import act.annotations.Label;
import act.data.annotation.Data;
import org.osgl.$;

@Data
public class Person {

    private String firstName;
    private String lastName;
    private Address address;
    private int age;

    public Person(String fn, String ln, Address addr, int age) {
        firstName = fn;
        lastName = ln;
        address = addr;
        this.age = age;
    }

    @Label("First name")
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

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void happyBirthday() {
        age++;
    }

    public int getAge() {
        return age;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Person) {
            Person that = (Person) obj;
            return $.eq(that.firstName, this.firstName) && $.eq(that.lastName, this.lastName) && $.eq(that.age, this.age);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
