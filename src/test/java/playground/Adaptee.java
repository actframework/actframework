package playground;

import org.osgl.util.S;

public class Adaptee {
    private static String s1 = "static string";
    private String s2;
    public Adaptee() {
        s2 = S.random();
    }

    private void say() {
        System.out.printf("%s and %s", s1, s2);
    }

    public static void main(String[] args) {
        Adaptee a = new Adaptee();
        a.say();
        a.say();
    }
}
