package act.i18n;

/**
 * Responsible for setting up client Locale for the context
 *
 * The client locale info is resolved in the following sequence:
 * 1. check the request parameter by configured name
 * 2. check the request URL path variable if path resolving is enabled
 * 3. check the `Accept-Language` header
 * 4. check the session variable
 * 5. use the server locale
 */
public class LocaleResolver {

}
