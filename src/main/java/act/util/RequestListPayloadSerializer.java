package act.util;

/**
 * Serialize list style (including array) payload to
 * request query or body. A list payload could
 * be serialized to request in different ways.
 *
 * For example, for a given array {1, 2, 3} with param name `foo`, it can be
 * serialized to request in multiple ways:
 *
 * * `foo=1&foo=2&foo=3`
 * * `foo[]=1&foo[]=2&foo[]=3`
 * * `foo[0]=1&foo[1]=2&foo[2]=3`
 *
 */
public enum RequestListPayloadSerializer {

}
