package act.handler.builtin;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.MockResponse;
import act.RequestImplBase;
import act.ActTestBase;
import act.app.ActionContext;
import act.controller.ParamNames;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgl.http.H;

import java.io.ByteArrayOutputStream;
import java.io.File;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StaticFileGetterTest extends ActTestBase {
    RequestImplBase req;
    ActionContext ctx;
    MockResponse resp;
    FileGetter pathHandler;
    FileGetter fileHandler;

    @Before
    public void prepare() throws Exception {
        super.setup();
        resp = new MockResponse();
        when(mockApp.file(anyString())).thenAnswer(new Answer<File>() {
            @Override
            public File answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String path = (String) args[0];
                return new File("target/test-classes", path);
            }
        });
        when(mockAppConfig.errorTemplatePathResolver()).thenCallRealMethod();
        req = mock(RequestImplBase.class);
        when(req.method()).thenReturn(H.Method.GET);
        ctx = ActionContext.create(mockApp, req, resp);
        when(req.context()).thenReturn(ctx);
        when(req.accept()).thenReturn(H.Format.HTML);
        pathHandler = new FileGetter("/public", mockApp);
        fileHandler = new FileGetter("/public/foo/bar.txt", mockApp);
        ctx.saveLocal();
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
