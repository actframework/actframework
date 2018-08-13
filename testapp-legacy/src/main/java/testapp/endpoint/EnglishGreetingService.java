package testapp.endpoint;

public class EnglishGreetingService implements GreetingService {
    @Override
    public String greet(String who) {
        return "Hi " + who;
    }
}
