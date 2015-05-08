package org.osgl.oms.util;

import org.osgl.exception.ConfigurationException;
import org.osgl.exception.FastRuntimeException;
import org.osgl.exception.UnexpectedException;
import org.osgl.util.C;

import java.util.Arrays;
import java.util.Comparator;
import java.util.IllegalFormatCodePointException;
import java.util.InvalidPropertiesFormatException;

/**
 * Used to sort Exception based on the inheritance hierarchy
 */
public class ExceptionComparator implements Comparator<Class<? extends Exception>> {
    @Override
    public int compare(Class<? extends Exception> o1, Class<? extends Exception> o2) {
        return hierarchicalLevel(o2) - hierarchicalLevel(o1);
    }

    private static int hierarchicalLevel(Class<? extends Exception> e) {
        int i = 0;
        Class<?> c = e;
        while (null != c) {
            i++;
            c = c.getSuperclass();
        }
        return i;
    }
}
