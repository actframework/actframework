package act.event;

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

import act.ActTestBase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class EventBusTest extends ActTestBase {

    private EventBus eventBus;

    @Before
    public void prepare() throws Exception {
        super.setup();
        eventBus = new EventBus(mockApp);
    }

    @Test
    public void listenerWithDifferentIdShallBeAddedOnlyAsManyTimesAsBound() throws Exception {
        ActEventListener<MyEmbeddedEvent> l1 = mock(ActEventListener.class);
        when(l1.id()).thenReturn("l1");
        ActEventListener<MyEmbeddedEvent> l2 = mock(ActEventListener.class);
        when(l1.id()).thenReturn("l2");
        eventBus.bind(MyEmbeddedEvent.class, l1);
        eventBus.bind(MyEmbeddedEvent.class, l2);
        MyEmbeddedEvent e = new MyEmbeddedEvent(this);
        eventBus.emit(e);
        verify(l1).on(e);
        verify(l1).on(e);
    }

    @Test
    public void listenerWithSameIdShallBeAddedOnlyOnce() throws Exception {
        ActEventListener<MyEmbeddedEvent> l1 = mock(ActEventListener.class);
        when(l1.id()).thenReturn("l");
        ActEventListener<MyEmbeddedEvent> l2 = mock(ActEventListener.class);
        when(l1.id()).thenReturn("l");
        eventBus.bind(MyEmbeddedEvent.class, l1);
        eventBus.bind(MyEmbeddedEvent.class, l2);
        MyEmbeddedEvent e = new MyEmbeddedEvent(this);
        eventBus.emit(e);
        verify(l1).on(e);
        verify(l1).on(e);
    }

    @Test
    public void itShallAllowUsingEmbeddedClassAsEventType() throws Exception {
        ActEventListener<MyEmbeddedEvent> l = mock(ActEventListener.class);
        eventBus.bind(MyEmbeddedEvent.class, l);
        eventBus.emit(new MyEmbeddedEvent(this));
        verify(l).on(Mockito.any(MyEmbeddedEvent.class));
    }


    public static class MyEmbeddedEvent extends ActEvent<EventBusTest> {
        public MyEmbeddedEvent(EventBusTest source) {
            super(source);
        }
    }
}
