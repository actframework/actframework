package testapp.endpoint;

import act.controller.Controller;
import org.osgl.http.H;
import org.osgl.mvc.annotation.Action;
import testapp.model.Bar;
import testapp.model.Contact;
import testapp.model.Foo;

import java.util.List;
import java.util.Map;

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

    @Action(value = "fooList")
    public List<Foo> fooList(List<Foo> fooList) {
        return fooList;
    }

    @Action(value = "fooArray")
    public Foo[] fooArray(Foo[] fooList) {
        return fooList;
    }

    @Action(value = "fooMap")
    public Map<String, Foo> fooMap(Map<String, Foo> fooMap) {
        return fooMap;
    }

    @Action("barMap")
    public Map<H.Method, Bar> barMap(Map<H.Method, Bar> barMap) {
        return barMap;
    }

    @Action("barMap2")
    public Map<Integer, Bar> barMapIntKey(Map<Integer, Bar> barMap) {
        return barMap;
    }

}
