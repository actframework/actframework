package ghissues;

public class Gh925 {

    public static class Super extends BaseController {}

    public static class Sub extends Super {

        public String test(String who) {
            return who;
        }

    }

}
