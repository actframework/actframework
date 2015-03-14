package org.osgl.util;

import org.hamcrest.Description;
import org.junit.internal.matchers.SubstringMatcher;
import org.mockito.ArgumentMatcher;

import java.io.Serializable;

public class NotStartsWith extends ArgumentMatcher<String> implements Serializable {
    private static final long serialVersionUID = -5978092285707998431L;
    private final String prefix;

    public NotStartsWith(String prefix) {
        this.prefix = prefix;
    }

    public boolean matches(Object actual) {
        return actual != null && !((String)actual).startsWith(this.prefix);
    }

    public void describeTo(Description description) {
        description.appendText("notStartsWith(\"" + this.prefix + "\")");
    }
}
