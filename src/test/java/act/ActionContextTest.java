package act;

import act.app.ActionContext;
import act.app.App;
import org.junit.Before;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.util.C;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActionContextTest extends TestBase {

    protected ActionContext ctx;

    @Before
    public void prepare() {
        H.Request req = mock(H.Request.class);
        when(req.paramNames()).thenReturn(C.list("foo", "bar"));
        when(req.paramVal("foo")).thenReturn("FOO");
        when(req.paramVal("bar")).thenReturn("BAR");
        when(req.paramVals("foo")).thenReturn(new String[]{"FOO", "foo"});
        when(req.paramVals("bar")).thenReturn(new String[]{"BAR", "bar"});

        H.Response resp = mock(H.Response.class);
        ctx = ActionContext.create(mock(App.class), req, resp);
    }

    @Test
    public void addParamToContext() {
        ctx.param("zoo", "ZOO");
        eq("ZOO", ctx.paramVal("zoo"));
    }

    @Test
    public void fetchReqParamVal() {
        eq("FOO", ctx.paramVal("foo"));
    }

    @Test
    public void fetchReqParamVals() {
        yes(Arrays.equals(new String[]{"FOO", "foo"}, ctx.paramVals("foo")));
    }

    @Test
    public void fetchAllParamMap() {
        Map<String, String[]> params = ctx.allParams();
        yes(params.containsKey("foo"));
        yes(C.listOf(params.get("foo")).contains("FOO"));
        yes(C.listOf(params.get("foo")).contains("foo"));
        yes(params.containsKey("bar"));
        no(params.containsKey("zoo"));
    }

    @Test
    public void fetchAllParamMapWithExtraParamAdded() {
        Map<String, String[]> params = ctx.allParams();
        yes(params.containsKey("foo"));
        yes(params.containsKey("bar"));
        no(params.containsKey("zoo"));
        ctx.param("zoo", "ZOO");
        params = ctx.allParams();
        yes(params.containsKey("foo"));
        yes(params.containsKey("bar"));
        yes(params.containsKey("zoo"));
        yes(C.listOf(params.get("zoo")).contains("ZOO"));
    }

    @Test
    public void extraParamOverrideReqParam() {
        eq("FOO", ctx.paramVal("foo"));
        eq(2, ctx.paramVals("foo").length);
        ctx.param("foo", "BAR");
        eq("BAR", ctx.paramVal("foo"));
        eq(1, ctx.paramVals("foo").length);
    }
}
