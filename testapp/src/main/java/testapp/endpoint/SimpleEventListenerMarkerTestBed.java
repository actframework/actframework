package testapp.endpoint;

import static testapp.endpoint.SimpleEventListenerMarkerTestBed.MyEvent.USER_LOGGED_IN;

import act.controller.Controller;
import act.event.EventBus;
import act.event.SimpleEventListener;
import org.osgl.exception.UnexpectedException;
import org.osgl.mvc.annotation.GetAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Singleton;

/**
 * Support {@link act.event.SimpleEventListener.Marker} test
 */
@Singleton
public class SimpleEventListenerMarkerTestBed extends Controller.Util {

    public enum MyEvent {
        USER_LOGGED_IN,
        USER_SIGNED_UP
    }

    @SimpleEventListener.Marker
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OnMyEvent {
        MyEvent[] value() default {};
    }

    private MyEvent eventReceived;

    @OnMyEvent(USER_LOGGED_IN)
    public void userLoggedIn() {
        eventReceived = USER_LOGGED_IN;
    }

    @GetAction("/event/custom_marker")
    public void test(EventBus eventBus) {
        eventReceived = null;
        eventBus.trigger(USER_LOGGED_IN);
        if (USER_LOGGED_IN == eventReceived) {
            ok();
        }
        throw new UnexpectedException("failed");
    }

}
