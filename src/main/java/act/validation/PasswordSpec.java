package act.validation;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * `PasswordSpec` provides a simple way to define {@link Password.Validator}.
 *
 * It supports validating the following trait of a password:
 * * check if the password contains a lowercase letter
 * * check if the password contains an uppercase letter
 * * check if the password contains a digit letter
 * * check if the password contains a special character
 * * check if the password length fall in specified range
 *
 * A `PasswordSpec` can be build using `PasswordSpec.Builder`:
 *
 * ```
 * PasswordSpec spec = PasswordSpec.builder()
 *      .requireLowercase()
 *      .requireUppercase()
 *      .minLength(5)
 *      .toPasswordSpec();
 * boolean isValid = spec.isValid("abcde"); // false as no uppercase letter found
 * isValid = spec.isValid("aBcde"); // true
 * isValid = spec.isValid("aBcd"); // false as length less than min length
 * ```
 *
 * It can also e created by parsing a password spec string:
 *
 * ```
 * PasswordSpec spec = PasswordSpec.parse("aA[5,])");
 * ```
 *
 * @see #parse(String)
 */
public class PasswordSpec implements Password.Validator {

    /**
     * Default minimum length of a password is `3`
     */
    public static final int DEF_MIN_LEN = 3;
    /**
     * Default maximum length of a password is unlimited
     */
    public static final int DEF_MAX_LEN = Integer.MAX_VALUE;
    /**
     * The following chars are considered to be special characters:
     *
     * <code>`~!@#$%^&*()[]{}'?</code>
     */
    public static final String SPECIAL_CHARS = "`~!@#$%^&*()[]{}'?";

    public static final char SPEC_LOWERCASE = 'a';
    public static final char SPEC_UPPERCASE = 'A';
    public static final char SPEC_DIGIT = '0';
    public static final char SPEC_SPECIAL_CHAR = '#';
    public static final char SPEC_LENSPEC_START = '[';
    public static final char SPEC_LENSPEC_CLOSE = ']';
    public static final char SPEC_LENSPEC_SEP = ',';

    private static final int BIT_LOWERCASE = 0x00001000;
    private static final int BIT_UPPERCASE = 0x00002000;
    private static final int BIT_DIGIT = 0x00003000;
    private static final int BIT_SPECIAL_CHAR = 0x00004000;

    private int minLength = DEF_MIN_LEN;
    private int maxLength = DEF_MAX_LEN;

    private int trait;

    private PasswordSpec() {}

    private PasswordSpec(PasswordSpec copy) {
        trait = copy.trait;
        minLength = copy.minLength;
        maxLength = copy.maxLength;
    }

    public boolean lowercaseRequired() {
        return (trait & BIT_LOWERCASE) != 0;
    }

    public boolean upppercaseRequired() {
        return (trait & BIT_UPPERCASE) != 0;
    }

    public boolean digitRequired() {
        return (trait & BIT_DIGIT) != 0;
    }

    public boolean specialCharRequired() {
        return (trait & BIT_SPECIAL_CHAR) != 0;
    }

    public int minLength() {
        return minLength;
    }

    public int maxLength() {
        return maxLength;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (lowercaseRequired()) {
            sb.append(SPEC_LOWERCASE);
        }
        if (upppercaseRequired()) {
            sb.append(SPEC_UPPERCASE);
        }
        if (digitRequired()) {
            sb.append(SPEC_DIGIT);
        }
        if (specialCharRequired()) {
            sb.append(SPEC_SPECIAL_CHAR);
        }
        sb.append("[").append(minLength).append(",");
        if (maxLength != DEF_MAX_LEN) {
            sb.append(maxLength);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean isValid(char[] password) {
        if (null == password) {
            return true;
        }
        int len = password.length;
        if (len < minLength() || len > maxLength()) {
            return false;
        }
        int trait = this.trait;
        for (int i = len - 1; i >= 0; --i) {
            char c = password[i];
            if (isLowercase(c)) {
                trait &= ~BIT_LOWERCASE;
            } else if (isUppercase(c)) {
                trait &= ~BIT_UPPERCASE;
            } else if (isDigit(c)) {
                trait &= ~BIT_DIGIT;
            } else if (isSpecialChar(c)) {
                trait &= ~BIT_SPECIAL_CHAR;
            }
        }
        return 0 == trait;
    }

    /**
     * A `PasswordSpec.Builder` is used to build a `PasswordSpec`.
     *
     * @see PasswordSpec
     */
    public static class Builder extends PasswordSpec {

        /**
         * Specify lowercase letter is required.
         *
         * @return the builder instance
         */
        public Builder requireLowercase() {
            super.trait |= BIT_LOWERCASE;
            return this;
        }

        /**
         * Specify uppercase letter is required.
         *
         * @return the builder instance
         */
        public Builder requireUppercase() {
            super.trait |= BIT_UPPERCASE;
            return this;
        }

        /**
         * Specify digit letter is required.
         *
         * @return the builder instance
         */
        public Builder requireDigit() {
            super.trait |= BIT_DIGIT;
            return this;
        }

        /**
         * Specify {@link #SPECIAL_CHARS special character} is required.
         *
         * @return the builder instance
         */
        public Builder requireSpecialChar() {
            super.trait |= BIT_SPECIAL_CHAR;
            return this;
        }

        /**
         * Specify minimum length of the password
         * @param len the minimum length
         * @return the builder instance
         */
        public Builder minLength(int len) {
            super.minLength = len;
            return this;
        }

        /**
         * Specify maximum length of the password
         * @param len the maximum length
         * @return the builder instance
         */
        public Builder maxLength(int len) {
            super.maxLength = len;
            return this;
        }

        /**
         * Return a {@link PasswordSpec} instance from this builder.
         *
         * The builder can be used to keep building other `PasswordSpec`
         * instance after calling this method
         *
         * @return the password spec built from this builder.
         */
        public PasswordSpec toPasswordSpec() {
            return new PasswordSpec(this);
        }
    }

    /**
     * Create a `PasswordSpec.Builder` instance.
     * @return a password spec builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parse a string representation of password spec.
     *
     * A password spec string should be `<trait spec><length spec>`.
     *
     * Where "trait spec" should be a composition of
     *
     * * `a` - indicate lowercase letter required
     * * `A` - indicate uppercase letter required
     * * `0` - indicate digit letter required
     * * `#` - indicate special character required
     *
     * "length spec" should be `[min,max]` where `max` can be omitted.
     *
     * Here are examples of valid "length spec":
     *
     * * `[6,20]` // min length: 6, max length: 20
     * * `[8,]` // min length: 8, max length: unlimited
     *
     * And examples of invalid "length spec":
     *
     * * `[8]` // "," required after min part
     * * `[a,f]` // min and max part needs to be decimal digit(s)
     * * `[3,9)` // length spec must be started with `[` and end with `]`
     *
     * @param spec a string representation of password spec
     * @return a {@link PasswordSpec} instance
     */
    public static PasswordSpec parse(String spec) {
        char[] ca = spec.toCharArray();
        int len = ca.length;
        illegalIf(0 == len, spec);
        Builder builder = new Builder();
        StringBuilder minBuf = new StringBuilder();
        StringBuilder maxBuf = new StringBuilder();
        boolean lenSpecStart = false;
        boolean minPart = false;
        for (int i = 0; i < len; ++i) {
            char c = ca[i];
            switch (c) {
                case SPEC_LOWERCASE:
                    illegalIf(lenSpecStart, spec);
                    builder.requireLowercase();
                    break;
                case SPEC_UPPERCASE:
                    illegalIf(lenSpecStart, spec);
                    builder.requireUppercase();
                    break;
                case SPEC_SPECIAL_CHAR:
                    illegalIf(lenSpecStart, spec);
                    builder.requireSpecialChar();
                    break;
                case SPEC_LENSPEC_START:
                    lenSpecStart = true;
                    minPart = true;
                    break;
                case SPEC_LENSPEC_CLOSE:
                    illegalIf(minPart, spec);
                    lenSpecStart = false;
                    break;
                case SPEC_LENSPEC_SEP:
                    minPart = false;
                    break;
                case SPEC_DIGIT:
                    if (!lenSpecStart) {
                        builder.requireDigit();
                    } else {
                        if (minPart) {
                            minBuf.append(c);
                        } else {
                            maxBuf.append(c);
                        }
                    }
                    break;
                default:
                    illegalIf(!lenSpecStart || !isDigit(c), spec);
                    if (minPart) {
                        minBuf.append(c);
                    } else {
                        maxBuf.append(c);
                    }
            }
        }
        illegalIf(lenSpecStart, spec);
        if (minBuf.length() != 0) {
            builder.minLength(Integer.parseInt(minBuf.toString()));
        }
        if (maxBuf.length() != 0) {
            builder.maxLength(Integer.parseInt(maxBuf.toString()));
        }
        return builder;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isLowercase(char c) {
        return c >= 'a' && c <= 'z';
    }

    private static boolean isUppercase(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static boolean isSpecialChar(char c) {
        return -1 < SPECIAL_CHARS.indexOf((int)c);
    }

    private static void illegalIf(boolean test, String spec) {
        if (test) {
            throw new IllegalArgumentException("Invalid password requirement spec:" + spec);
        }
    }

}
