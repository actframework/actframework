package act.controller.captcha;

public interface CaptchaSessionGenerator {

    /**
     * Generate a random {@link CaptchaSession}
     * @return the CAPTCHA session generated
     */
    CaptchaSession generate();

}
