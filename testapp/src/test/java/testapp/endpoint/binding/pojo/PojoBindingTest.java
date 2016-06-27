package testapp.endpoint.binding.pojo;

import org.junit.Test;
import org.osgl.util.C;
import org.osgl.util.KVStore;
import testapp.endpoint.binding.ActionParameterBindingTestBase;
import testapp.model.Contact;

public class PojoBindingTest extends ActionParameterBindingTestBase {

    @Override
    protected String urlContext() {
        return "/pojo";
    }

    @Test
    public void postFullContactMethodFormData() {
        Contact contact = prepareFullContact();

    }

    protected KVStore prepareNonEmptyKvStore() {
        KVStore kv = new KVStore();
        kv.putValue("foo", "bar");
        kv.putValue("n", 10);
        return kv;
    }

    protected Contact prepareFullContact() {
        Contact contact = new Contact();
        contact.setAddress("addr1");
        contact.setEmail("who@where");
        contact.setPhone("010101");
        contact.setId("123");
        contact.setEmails(C.newSet(C.list("a@where", "b@where")));
        contact.setKv(prepareNonEmptyKvStore());
        return contact;
    }

}
