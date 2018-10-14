package testapp.endpoint;

import org.osgl.mvc.annotation.Action;

/**
 * This is to test controller request param binding to
 * both parameter and field
 */
public class ParamAndField {

    public static final String PATH = "param_vs_field";

    String bar;

    @Action(PATH)
    public String concatenate(String foo) {
        return foo + bar;
    }

}
