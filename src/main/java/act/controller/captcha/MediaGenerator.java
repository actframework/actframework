package act.controller.captcha;

/**
 * Generate media stream for a CAPTCHA token
 */
public interface MediaGenerator {
    void generate(String token);
}
