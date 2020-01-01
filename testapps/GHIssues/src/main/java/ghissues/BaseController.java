package ghissues;

import act.handler.NoReturnValueAdvice;
import act.util.LogSupport;
import org.osgl.aaa.NoAuthentication;

@NoReturnValueAdvice
@NoAuthentication
public class BaseController extends LogSupport {
}
