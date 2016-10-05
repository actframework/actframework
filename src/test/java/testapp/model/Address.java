package testapp.model;

import act.data.annotation.Data;
import org.osgl.$;

@Data
public class Address {
    private String streetNo;
    private String streetName;
    private String city;

    public Address(String streetNo, String streetName, String city) {
        this.streetNo = streetNo;
        this.streetName = streetName;
        this.city = city;
    }

    public String getStreetNo() {
        return streetNo;
    }

    public void setStreetNo(String streetNo) {
        this.streetNo = streetNo;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public int hashCode() {
        return $.hc(streetNo, streetName, city);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Address) {
            Address that = (Address) obj;
            return $.eq(that.streetNo, this.streetNo) && $.eq(that.streetName, this.streetName) && $.eq(that.city, this.city);
        }
        return false;
    }
}
