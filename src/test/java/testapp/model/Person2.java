package testapp.model;

import act.data.Data;
import act.util.EqualIgnore;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.N;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.List;

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
