package org.osgl.mvc.server.route;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgl.http.H;
import org.osgl.http.util.Path;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.server.AppContext;
import org.osgl.mvc.server.TestBase;
import org.osgl.util.Unsafe;

import java.util.List;

public class RouteFileTreeBuilderTest extends TestBase {

    private Tree tree;
    private RouteFileTreeBuilder builder;
    private AppContext ctx;
    private static final String SVC_ID_ALL_ONE = "111111111111111111111111";

    public RouteFileTreeBuilderTest() {
        tree = new Tree();
        String line1 = "* / Application.home";
        String line2 = "POST /service Application.addService";
        String line3 = "PUT /service/{<[0-9]{24}>id} Application.updateService";
        builder = new RouteFileTreeBuilder(line1, line2, line3);
        builder.setInvokerResolver(new MockActionInvokerResolver());
        builder.build(tree);
    }

    @Before
    public void prepare() {
        ctx = Mockito.mock(AppContext.class);
    }

    void assertInvoke(String expected, H.Method method, String url) {
        List<CharSequence> path = Path.tokenize(Unsafe.bufOf(url));
        tree.getInvoker(method, path, ctx).invoke(ctx);
        eq(expected, MockInvoker.getResult());
    }

    @Test(expected = NotFound.class)
    public void invokeOnUnknownPath() {
        assertInvoke("whatever", H.Method.GET, "/service");
    }

    @Test
    public void invokeOnHomeUrl() {
        assertInvoke("Application.home", H.Method.GET, "/");
        assertInvoke("Application.home", H.Method.POST, "/");
        assertInvoke("Application.home", H.Method.PUT, "/");
        assertInvoke("Application.home", H.Method.DELETE, "/");
    }

    @Test
    public void invokeOnServicePost() {
        assertInvoke("Application.addService", H.Method.POST, "/service");
    }

    @Test
    public void invokeOnUpdateService() {
        assertInvoke("Application.updateService", H.Method.PUT, "/service/" + SVC_ID_ALL_ONE);
        Mockito.verify(ctx).param("id", SVC_ID_ALL_ONE);
    }

    @Test(expected = NotFound.class)
    public void invokeOnUpdateServiceWithWrongServiceId() {
        assertInvoke("Application.updateService", H.Method.PUT, "/service/" + SVC_ID_ALL_ONE + "13");
    }
}
