package testapp.sbean;

public class NotSimpleBeanWithoutDefaultConstructor {

    public String propertyWithoutGetter;

    public NotSimpleBeanWithoutDefaultConstructor(String s) {
        propertyWithoutGetter = s;
    }

    public void setPropertyWithoutGetter(String s) {
        propertyWithoutGetter = s;
    }

}
