package testapp.model;

import act.data.Data;

@Data
public class Address2 {
    public static String foo;
    private String streetNo;
    private String streetName;
    private String city;
    private transient boolean bar;

    public Address2(String streetNo, String streetName, String city) {
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
}
