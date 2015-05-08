package sparkapp;

import org.osgl.exception.UnexpectedException;
import org.osgl.exception.UnexpectedIOException;
import org.osgl.http.H;
import org.osgl.oms.app.AppContext;
import org.osgl.oms.boot.spark.SparkApp;
import org.osgl.util.S;

import java.io.IOException;
import java.util.Random;

import static org.osgl.oms.boot.spark.SparkApp.*;
import static org.osgl.oms.controller.Controller.Util.*;

/**
 * Demonstrate how to use Spark style exception handlers
 */
public class ExceptionHandlerDemo {

    public static class FragileMomentException extends UnexpectedException {
    }

    public static class SuperIOException extends UnexpectedIOException {
        public SuperIOException() {
            super(new IOException("ddd"));
        }
    }

    public static void main(String[] args) {
        get("/", echo("hello world"));
        get("/some/fragile/place", new Handler() {
            @Override
            public void handle(H.Request req, H.Response resp) {
                int i = new Random().nextInt(3);
                switch (i) {
                    case 2:
                        throw new FragileMomentException();
                    case 1:
                        throw new SuperIOException();
                    case 0:
                        throw new RuntimeException();
                    default:
                        throw new UnexpectedException();
                }
            }
        });

        on(FragileMomentException.class, echo("fragile moment!"));
        on(SuperIOException.class, new Handler() {
            @Override
            public void handle(AppContext context) {
                context.flash().error("super IO exception!!!");
            }
        });
        on(RuntimeException.class, new Handler() {
            @Override
            public void handle(AppContext context) {
                String error = context.flash().error();
                if (S.notBlank(error)) {
                    context.resp().writeContent(S.builder("runtime exception: ").append(error).toString());
                } else {
                    context.resp().writeContent("Unknown runtime exception!");
                }
            }
        });
    }

}
