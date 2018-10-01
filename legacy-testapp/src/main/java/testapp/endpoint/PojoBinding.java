package testapp.endpoint;

import act.controller.annotation.UrlContext;
import org.osgl.http.H;
import org.osgl.inject.annotation.SessionScoped;
import org.osgl.mvc.annotation.*;
import org.osgl.util.C;
import testapp.model.*;

import java.util.List;
import java.util.Map;

/**
 * Test binding to a POJO object
 */
@UrlContext("/pojo")
@SuppressWarnings("unused")
public class PojoBinding {

    private Foo foo;

    @SessionFree
    @Action(value = "ctct", methods = {H.Method.POST, H.Method.PUT})
    public Contact createContact(Contact contact) {
        return contact;
    }

    @SessionFree
    @Action(value = "foo", methods = {H.Method.POST, H.Method.PUT, H.Method.GET})
    public Foo foo() {
        return foo;
    }

    @SessionFree
    @Action("foobar")
    public List<Object> fooBar(Bar bar) {
        return C.list(foo, bar);
    }

    @SessionFree
    @Before(only = "fooList")
    public void beforeFooList(List<Foo> fooList) {
        System.out.println(fooList);
    }

    @SessionFree
    @Action(value = "fooList")
    public List<Foo> fooList(List<Foo> fooList) {
        return fooList;
    }

    @Action("bar1")
    public Bar bar1(@SessionScoped Bar bar) {
        return bar;
    }

    @SessionFree
    @Action("bar0")
    public Bar barIndependent(Bar bar) {
        return bar;
    }

    @SessionFree
    @Action(value = "fooArray")
    public Foo[] fooArray(Foo[] fooList) {
        return fooList;
    }

    @SessionFree
    @Action(value = "fooMap")
    public Map<String, Foo> fooMap(Map<String, Foo> fooMap) {
        return fooMap;
    }

    @SessionFree
    @Action("barMap")
    public Map<H.Method, Bar> barMap(Map<H.Method, Bar> barMap) {
        return barMap;
    }

    @SessionFree
    @Action("barMap2")
    public Map<Integer, Bar> barMapIntKey(Map<Integer, Bar> barMap) {
        return barMap;
    }

}
