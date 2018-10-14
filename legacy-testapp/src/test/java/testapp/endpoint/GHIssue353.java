package testapp.endpoint;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.util.C;
import testapp.endpoint.ghissues.gh353.User;

import java.io.IOException;
import java.util.Map;

public class GHIssue353 extends EndpointTester {

    protected User user = new User("Tom Jerry");

    private void resetDb() throws IOException {
        url("/gh/353").delete();
        checkRespCode();
        reset();
    }

    @Override
    protected void _setup() throws IOException {
        resetDb();
    }

    @Test
    public void testPost() throws Exception {
        url("/gh/353").accept(H.Format.JSON).postJSON(user);
        String s = resp().body().string();
        User user2 = JSON.parseObject(s, User.class);
        eq(user.name, user2.name);
    }

    @Test
    public void testPut() throws Exception {
        url("/gh/353").accept(H.Format.JSON).postJSON(user);
        String s = resp().body().string();
        User user2 = JSON.parseObject(s, User.class);
        assertNotNull(user2._id());
        checkRespCode();
        reset();
        Map<String, String> updates = C.Map("name", "Donald Mickey");
        url("/gh/353/" + user2.getIdAsStr()).accept(H.Format.JSON).postJSON(updates).put();
        s = resp().body().string();
        User user3 = JSON.parseObject(s, User.class);
        eq(user2.getId(), user3.getId());
        eq("Donald Mickey", user3.name);
    }

}
