package act.route;

import act.TestBase;
import act.app.ActionContext;
import act.app.App;
import act.conf.AppConfig;
import act.handler.RequestHandler;
import act.handler.RequestHandlerResolver;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;

import static org.mockito.Mockito.*;

public abstract class RouterTestBase extends TestBase {

    protected Router router;
    protected RequestHandler controller;
    protected RequestHandlerResolver controllerLookup;
    protected ActionContext ctx;
    protected App app;
    protected static final File BASE = new File("target/test-classes");

    @Before
    public void _prepare() {
        controller = mock(NamedMockHandler.class);
        controllerLookup = mock(RequestHandlerResolver.class);
        provisionControllerLookup(controllerLookup);
        AppConfig config = appConfig();
        app = mock(App.class);
        when(app.config()).thenReturn(config);
        when(app.file(anyString())).thenAnswer(new Answer<File>() {
            @Override
            public File answer(InvocationOnMock invocation) throws Throwable {
                String path = (String) invocation.getArguments()[0];
                return new File(BASE, path);
            }
        });
        router = new Router(controllerLookup, app);
        buildRouteMapping(router);
        ctx = mock(ActionContext.class);
        when(ctx.app()).thenReturn(app);
    }

    protected void provisionControllerLookup(RequestHandlerResolver controllerLookup) {
        when(controllerLookup.resolve(anyString(), any(App.class))).thenReturn(controller);
    }

    protected abstract void buildRouteMapping(Router router);

    protected AppConfig appConfig() {
        return new AppConfig();
    }

    protected void controllerInvoked() {
        verify(controller).handle(ctx);
    }

}
