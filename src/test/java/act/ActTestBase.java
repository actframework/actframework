package act;

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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import act.app.ActionContext;
import act.app.App;
import act.app.SingletonRegistry;
import act.conf.AppConfig;
import act.event.EventBus;
import act.job.JobManager;
import act.metric.SimpleMetricPlugin;
import act.route.Router;
import act.session.SessionManager;
import act.util.ClassNames;
import act.util.IdGenerator;
import org.junit.Ignore;
import org.junit.runner.JUnitCore;
import org.mockito.Matchers;
import org.mockito.internal.matchers.StartsWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgl.$;
import org.osgl.cache.CacheService;
import org.osgl.http.H;
import org.osgl.util.FastStr;
import org.osgl.util.IO;
import org.osgl.util.S;
import osgl.ut.TestBase;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;

@Ignore
public class ActTestBase extends TestBase {

    protected void ceq(Object o1, Object o2) {
        assertEquals(S.string(o1), S.string(o2));
    }

    protected static void run(Class<? extends ActTestBase> cls) {
        new JUnitCore().run(cls);
    }

    protected static void println(String tmpl, Object... args) {
        System.out.println(String.format(tmpl, args));
    }

    protected static <T> T fieldVal(Object entity, String field) {
        return $.getProperty(entity, field);
    }

    public static File root() {
        FastStr fs = FastStr.of(ActTestBase.class.getClassLoader().getResource("routes").getPath());
        FastStr classRoot = fs.beforeLast("/");
        FastStr target = classRoot.beforeLast("/");
        return new File(target.toString());
    }

    protected Router mockRouter;
    protected ActionContext mockActionContext;
    protected AppConfig mockAppConfig;
    protected JobManager mockJobManager;
    protected SingletonRegistry mockSingletonRegistry;
    protected App mockApp;
    protected EventBus mockEventBus;
    protected H.Request mockReq;
    protected ActResponse mockResp;
    protected CacheService mockCacheService;
    protected IdGenerator idGenerator;

    protected void setup() throws Exception {
        initActMetricPlugin();
        idGenerator = new IdGenerator();
        mockApp = mock(App.class);
        Field f = App.class.getDeclaredField("INST");
        f.setAccessible(true);
        f.set(null, mockApp);
        mockSingletonRegistry = mock(SingletonRegistry.class);
        //when(mockApp.singletonRegistry()).thenReturn(mockSingletonRegistry);
        mockJobManager = mock(JobManager.class);
        when(mockApp.jobManager()).thenReturn(mockJobManager);
        mockEventBus = mock(EventBus.class);
        when(mockApp.eventBus()).thenReturn(mockEventBus);
        mockAppConfig = mock(AppConfig.class);
        when(mockAppConfig.possibleControllerClass(argThat(new StartsWith("testapp.controller.")))).thenReturn(true);
        mockActionContext = mock(ActionContext.class);
        when(mockActionContext.app()).thenReturn(mockApp);
        when(mockActionContext.config()).thenReturn(mockAppConfig);
        mockRouter = mock(Router.class);
        when(mockApp.config()).thenReturn(mockAppConfig);
        when(mockApp.router()).thenReturn(mockRouter);
        when(mockApp.router(Matchers.same(""))).thenReturn(mockRouter);
        when(mockApp.cuid()).thenReturn(idGenerator.genId());
        mockCacheService = mock(CacheService.class);
        when(mockApp.getInstance(any(Class.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Class<?> cls = (Class) args[0];
                if (SessionManager.class == cls) {
                    return new SessionManager(mockAppConfig, mockCacheService);
                }
                return $.newInstance((Class)args[0]);
            }
        });
        mockReq = mock(H.Request.class);
        mockResp = mock(ActResponse.class);
        when(mockReq.method()).thenReturn(H.Method.GET);
    }

    protected byte[] loadBytecode(String className) {
        String fileName = ClassNames.classNameToClassFileName(className);
        InputStream is = getClass().getResourceAsStream(fileName);
        return IO.readContent(is);
    }

    private void initActMetricPlugin() throws Exception {
        Field f = Act.class.getDeclaredField("metricPlugin");
        f.setAccessible(true);
        f.set(null, new SimpleMetricPlugin());
    }

}
