package org.osgl.oms.action.builtin;

import org.junit.Before;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;
import org.osgl.oms.*;
import org.osgl.oms.app.App;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.conf.AppConfig;
import org.osgl.oms.controller.ParamNames;

import java.io.ByteArrayOutputStream;

import static org.mockito.Mockito.mock;

public class StaticFileGetterTest extends TestBase {
    AppContext ctx;
    H.Response resp;
    StaticFileGetter pathHandler;
    StaticFileGetter fileHandler;

    @Before
    public void prepare() {
        resp = new MockResponse();
        AppConfig cfg = mock(AppConfig.class);
        App app = mock(App.class);
        RequestImplBase req = mock(RequestImplBase.class);
        ctx = AppContext.create(app, req, resp);
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
