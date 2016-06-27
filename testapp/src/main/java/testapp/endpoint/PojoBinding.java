package testapp.endpoint;

import act.controller.Controller;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;
import testapp.model.Contact;

/**
 * Test binding to a POJO object
 */
@Controller("/pojo")
public class PojoBinding {

    @Action(value = "ctct", methods = {H.Method.POST, H.Method.PUT})
    public Contact createContact(Contact contact) {
        return contact;
    }

}
