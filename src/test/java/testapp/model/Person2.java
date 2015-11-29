package testapp.model;

import act.util.AutoObject;
import act.util.EqualIgnore;
import org.osgl.util.S;

@AutoObject
public class Person2 {

    @EqualIgnore
    private String v1;
    private String firstName;
    private String lastName;
    private Address2 address;
    private int age;

    public Person2(String fn, String ln, Address2 addr, Integer age) {
        firstName = fn;
        lastName = ln;
        address = addr;
        this.age = age;
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

    public void happyBirthday() {
        age++;
    }
}
