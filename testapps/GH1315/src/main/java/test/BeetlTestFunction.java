package test;

import org.beetl.core.Context;
import org.beetl.core.Function;
import org.osgl.util.S;

import javax.inject.Named;

@Named("myFunc")
public class BeetlTestFunction implements Function {

    @Override
    public Object call(Object[] objects, Context context) {
        String value = (String) objects[0];
        String ovalue = (String) objects[1];
        if(S.eq(value, ovalue, S.IGNORECASE)){
            return "checked";
        }
        return "";
    }
}