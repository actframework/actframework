package act.handler.builtin;

import act.MockResponse;
import act.RequestImplBase;
import act.TestBase;
import act.app.App;
import act.app.AppContext;
import act.conf.AppConfig;
import act.controller.ParamNames;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgl.http.H;
import org.osgl.mvc.result.NotFound;

import java.io.ByteArrayOutputStream;
import java.io.File;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StaticFileGetterTest extends TestBase {
    AppContext ctx;
    MockResponse resp;
    StaticFileGetter pathHandler;
    StaticFileGetter fileHandler;

    @Before
    public void prepare() {
        resp = new MockResponse();
        App app = mock(App.class);
        when(app.file(anyString())).thenAnswer(new Answer<File>() {
            @Override
            public File answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String path = (String) args[0];
                return new File("target/test-classes", path);
            }
        });
        RequestImplBase req = mock(RequestImplBase.class);
        ctx = AppContext.create(app, req, resp);
        pathHandler = new StaticFileGetter("/public", app);
        fileHandler = new StaticFileGetter("/public/foo/bar.txt", app);
    }

    @Test
    public void invokePathHandlerOnNonExistingResource() {
        ctx.param(ParamNames.PATH, "/some/where/non_exists.txt");
        pathHandler.handle(ctx);
        eq(resp.status, 404);
    }

    @Test
    public void invokePathHandlerOnExistingResource() {
        ctx.param(ParamNames.PATH, "/foo/bar.txt");
        pathHandler.handle(ctx);
        ByteArrayOutputStream baos = (ByteArrayOutputStream)resp.outputStream();
        String s = new String(baos.toByteArray());
        ceq("foo/bar.txt", s);
    }

    @Test
    public void invokePathHandlerOnExistingResource2() {
        // this time use relative path
        ctx.param(ParamNames.PATH, "foo/bar.txt");
        pathHandler.handle(ctx);
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
        fileHandler.handle(ctx);
        ByteArrayOutputStream baos = (ByteArrayOutputStream)resp.outputStream();
        String s = new String(baos.toByteArray());
        ceq("foo/bar.txt", s);
    }

    @Test
    public void pathParameterShallBeIgnoredWhenBaseIsFile() {
        ctx.param(ParamNames.PATH, "some/thing/should/be/ignored.txt");
        fileHandler.handle(ctx);
        ByteArrayOutputStream baos = (ByteArrayOutputStream)resp.outputStream();
        String s = new String(baos.toByteArray());
        ceq("foo/bar.txt", s);
    }

}
