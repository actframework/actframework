package testapp.model;

import act.util.Data;
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
