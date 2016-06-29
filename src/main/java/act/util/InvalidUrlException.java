package act.util;

/**
 * Thrown out if {@link UrlBuilder#parse(String)} found the string specified
 * is not a valid URL
 */
public class InvalidUrlException extends IllegalArgumentException {
    private String url;

    public InvalidUrlException(String url) {
        this.url = url;
    }

    @Override
    public String getMessage() {
        return "Invalid URL: " + url;
    }

    public String getUrl() {
        return url;
    }
}
