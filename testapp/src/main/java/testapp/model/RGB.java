package testapp.model;

import org.osgl.util.C;

import java.util.List;
import java.util.TreeSet;

/**
 * Used for testing enum type binding
 */
public enum RGB {
    R, G, B;

    public static void main(String[] args) {
        List<RGB> l = C.list(R, B, G);
        System.out.println(new TreeSet<>(l));
    }
}
