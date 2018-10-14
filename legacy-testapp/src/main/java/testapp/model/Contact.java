package testapp.model;

import act.data.annotation.Data;
import org.osgl.util.C;

import java.util.Collection;
import java.util.Set;

/**
 * A Contact
 */
@Data
public class Contact extends ModelBase {

    private String address;
    private String phone;
    private Set<String> emails;
    private String email;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEmails(Collection<String> emails) {
        this.emails = C.newSet(emails);
    }

    public Set<String> getEmails() {
        return emails;
    }
}
