package testapp.sbean;

import act.util.SimpleBean;

public class SimpleBeanWithoutDefaultConstructor implements SimpleBean {

    public String propertyWithoutGetter;

    public int magicNumber;

    public boolean active;

    public boolean magicFlag;

    public boolean weirdFlag;

    public int propertyWithoutSetter;

    public float propertyWithoutGetterAndSetter;

    public SimpleBeanWithoutDefaultConstructor(String s) {
        propertyWithoutGetter = s;
    }

    public void setPropertyWithoutGetter(String s) {
        propertyWithoutGetter = s;
    }

    public int getPropertyWithoutSetter() {
        return propertyWithoutSetter;
    }

    public void setMagicNumber(int n) {
        magicNumber = n;
    }

    public int getMagicNumber() {
        return magicNumber * 2;
    }

    public boolean isMagicFlag() {
        return !magicFlag;
    }

    public void setWeirdFlag(boolean flag) {
        weirdFlag = !flag;
    }

}
