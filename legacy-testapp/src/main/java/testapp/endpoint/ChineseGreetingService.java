package testapp.endpoint;

public class ChineseGreetingService implements GreetingService {
    @Override
    public String greet(String who) {
        return who + "你好";
    }
}
