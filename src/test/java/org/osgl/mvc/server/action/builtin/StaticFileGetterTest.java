package org.osgl.mvc.server.action.builtin;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.mvc.server.*;

import java.io.ByteArrayOutputStream;

public class StaticFileGetterTest extends TestBase {
    AppContext ctx;
    H.Response resp;
    StaticFileGetter pathHandler;
    StaticFileGetter fileHandler;

    @Before
    public void prepare() {
        resp = new MockResponse();
        AppConfig cfg = Mockito.mock(AppConfig.class);
        RequestImplBase req = Mockito.mock(RequestImplBase.class);
        ctx = new AppContext(cfg, req, resp);
        pathHandler = new StaticFileGetter("/public");
        fileHandler = new StaticFileGetter("/public/foo/bar.txt", true);
    }

    @Test(expected = NotFound.class)
    public void invokePathHandlerOnNonExistingResource() {
        ctx.param(ParamNames.PATH, "/some/where/non_exists.txt");
        pathHandler.invoke(ctx);
    }

    @Test
    public void invokePathHandlerOnExistingResource() {
        ctx.param(ParamNames.PATH, "/foo/bar.txt");
        pathHandler.invoke(ctx);
        ByteArrayOutputStream baos = (ByteArrayOutputStream)resp.outputStream();
        String s = new String(baos.toByteArray());
        ceq("foo/bar.txt", s);
    }

    @Test
    public void invokePathHandlerOnExistingResource2() {
        // this time use relative path
        ctx.param(ParamNames.PATH, "foo/bar.txt");
        pathHandler.invoke(ctx);
        ByteArrayOutputStream baos = (ByteArrayOutputStream)resp.outputStream();
        String s = new String(baos.toByteArray());
        ceq("foo/bar.txt", s);
    }

    @Test
    public void pathHandlerShallSupportPartialPath() {
        yes(pathHandler.supportPartialPath());
    }

    @Test
    public void fileHandlerShallNotSupportPartialPath() {
        no(fileHandler.supportPartialPath());
    }

    @Test
    public void invokeFileHandler() {
        fileHandler.invoke(ctx);
        ByteArrayOutputStream baos = (ByteArrayOutputStream)resp.outputStream();
        String s = new String(baos.toByteArray());
        ceq("foo/bar.txt", s);
    }

    @Test
    public void pathParameterShallBeIgnoredWhenBaseIsFile() {
        ctx.param(ParamNames.PATH, "some/thing/should/be/ignored.txt");
        fileHandler.invoke(ctx);
        ByteArrayOutputStream baos = (ByteArrayOutputStream)resp.outputStream();
        String s = new String(baos.toByteArray());
        ceq("foo/bar.txt", s);
    }

}
