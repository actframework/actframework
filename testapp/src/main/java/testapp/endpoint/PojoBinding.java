package testapp.endpoint;

import act.controller.Controller;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;
import testapp.model.Contact;
import testapp.model.Foo;

/**
 * Test binding to a POJO object
 */
@Controller("/pojo")
public class PojoBinding {

    @Action(value = "ctct", methods = {H.Method.POST, H.Method.PUT})
    public Contact createContact(Contact contact) {
        return contact;
    }

    @Action(value = "foo", methods = {H.Method.POST, H.Method.PUT, H.Method.GET})
    public Foo foo(Foo foo) {
        return foo;
    }

}
