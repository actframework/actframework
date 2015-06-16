package act.app.security;

public interface SecurityContext {
    /**
     * Returns the principal that represent the current interaction subject
     */
    Object principal();
}
