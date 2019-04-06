package ghissues;

import act.app.util.SimpleRestfulServiceBase;
import act.controller.annotation.UrlContext;
import act.handler.NoReturnValueAdvice;
import ghissues.gh976.Foo;
import org.osgl.aaa.NoAuthentication;

@UrlContext("976")
@NoReturnValueAdvice
@NoAuthentication
public class Gh976 extends SimpleRestfulServiceBase<Integer, Foo, Foo.Dao> {
}
