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

import org.junit.Test;
import osgl.ut.TestBase;

import static act.validation.PasswordSpec.DEF_MAX_LEN;
import static act.validation.PasswordSpec.DEF_MIN_LEN;
import static act.validation.PasswordSpec.parse;

public class PasswordSpecTest extends TestBase {

    @Test
    public void testFullSpecString() {
        PasswordSpec spec = parse("aA0#[6,9]");
        yes(spec.digitRequired());
        yes(spec.lowercaseRequired());
        yes(spec.specialCharRequired());
        yes(spec.upppercaseRequired());
        eq(6, spec.minLength());
        eq(9, spec.maxLength());
    }

    @Test
    public void testFullSpecStringWithAbnormalLenSpec() {
        PasswordSpec spec = parse("aA[6,9]0#");
        yes(spec.digitRequired());
        yes(spec.lowercaseRequired());
        yes(spec.specialCharRequired());
        yes(spec.upppercaseRequired());
        eq(6, spec.minLength());
        eq(9, spec.maxLength());
    }

    @Test
    public void testSpecStrWithoutLenSpec() {
        PasswordSpec spec = parse("aA0#");
        yes(spec.digitRequired());
        yes(spec.lowercaseRequired());
        yes(spec.specialCharRequired());
        yes(spec.upppercaseRequired());
        eq(DEF_MIN_LEN, spec.minLength());
        eq(DEF_MAX_LEN, spec.maxLength());
    }

    @Test
    public void testSpecStrWithOnlyLenSpec() {
        PasswordSpec spec = parse("[6,9]");
        no(spec.digitRequired());
        no(spec.lowercaseRequired());
        no(spec.specialCharRequired());
        no(spec.upppercaseRequired());
        eq(6, spec.minLength());
        eq(9, spec.maxLength());
    }

    @Test
    public void testSpecStrWithoutSpecialCharRequired() {
        PasswordSpec spec = parse("aA0");
        yes(spec.digitRequired());
        yes(spec.lowercaseRequired());
        no(spec.specialCharRequired());
        yes(spec.upppercaseRequired());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFeatureCode() {
        parse("34");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFeatureCode2() {
        parse("X");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnclosedLenSpec() {
        parse("aA[11,");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLenSpec() {
        parse("abc[12]");
    }

    @Test
    public void testLenSpecWithoutMaxLen() {
        PasswordSpec spec = parse("[6,]");
        eq(6, spec.minLength());
        eq(PasswordSpec.DEF_MAX_LEN, spec.maxLength());
    }

    @Test
    public void testValidateLength() {
        PasswordSpec spec = parse("[6,9]");
        yes(spec.isValid("111111".toCharArray()));
        yes(spec.isValid("111111111".toCharArray()));
        no(spec.isValid("111".toCharArray()));
        no(spec.isValid("1111111111".toCharArray()));
    }

    @Test
    public void testFeature() {
        PasswordSpec spec = parse("Aa[1,]");
        yes(spec.isValid("xY".toCharArray()));
        no(spec.isValid("xx".toCharArray()));
        spec = parse("0#[1,]");
        yes(spec.isValid("1~".toCharArray()));
        no(spec.isValid("abcD0".toCharArray()));
    }

    @Test
    public void testToString() {
        PasswordSpec spec = parse("0Aa");
        eq("aA0[3,]", spec.toString());
    }

}
