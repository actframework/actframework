package testapp.endpoint;

import act.controller.Controller;
import org.osgl.mvc.annotation.Action;
import org.osgl.util.S;

/**
 * Used to test parameter resolving
 */
public class ParameterResolver extends Controller.Util {

    @Action("/pr/char_p")
    public char takeCharP(char c) {
        return c;
    }

    @Action("/pr/char")
    public Character takeChar(Character c) {
        return c;
    }


    @Action("/pr/int_p")
    public int takeIntP(int i) {
        return i;
    }

    @Action("/pr/int")
    public Integer takeInt(Integer i) {
        return i;
    }

}
