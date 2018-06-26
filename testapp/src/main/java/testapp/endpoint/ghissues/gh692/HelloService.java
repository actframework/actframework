package testapp.endpoint.ghissues.gh692;

public class HelloService implements Gh692Service {
    @Override
    public String name() {
        return "Hello";
    }
}
