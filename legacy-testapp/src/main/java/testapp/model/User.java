package testapp.model;

/**
 * A model used to test parameter binding
 */
public class User extends ModelBase {

    private String name;
    private int age;
    private float f;
    private double d;

    private Contact contact;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public float getF() {
        return f;
    }

    public void setF(float f) {
        this.f = f;
    }

    public double getD() {
        return d;
    }

    public void setD(double d) {
        this.d = d;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

}
