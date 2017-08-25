package testapp.endpoint.binding.pojo;

import com.alibaba.fastjson.JSON;
import org.junit.Ignore;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.KVStore;
import testapp.endpoint.EndPointTestContext;
import testapp.endpoint.binding.ActionParameterBindingTestBase;
import testapp.model.Contact;

public class PojoBindingTest extends ActionParameterBindingTestBase {

    @Override
    protected String urlContext() {
        return "/pojo";
    }

    @Ignore
    @Test
    public void postFullContactMethodFormData() {
        Contact contact = prepareFullContact();
        // TODO finish form data test
    }

    @Test
    public void postFullContactJsonBody() throws Exception {
        Contact contact = prepareFullContact();
        context.expected(JSON.toJSONString(contact))
                .url(processUrl("ctct"))
                .accept(H.Format.JSON)
                .jsonBody(C.map("contact", contact))
                .method(EndPointTestContext.RequestMethod.POST_JSON)
                .applyTo(this);
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
