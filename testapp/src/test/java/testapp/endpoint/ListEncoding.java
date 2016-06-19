package testapp.endpoint;

import java.util.List;

/**
 * There are three ways to pass array/list element in GET request. For detail refers to
 * http://stackoverflow.com/questions/11889997/how-to-send-a-array-in-url-request
 */
public enum ListEncoding {
    ONE() {
        
    }, TWO, THREE;

    public abstract String encode(String bindName, List<?> elements);
}
