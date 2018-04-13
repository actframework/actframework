package me.tongfei.progressbar;

/*-
 * #%L
 * TongFei ProgressBar
 * %%
 * Copyright (C) 2014 - 2018 Tongfei Chen
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


import org.joda.time.Duration;

/**
 * @author Tongfei Chen
 * @since 0.5.0
 */
public class Util {

    static String repeat(char c, int n) {
        if (n <= 0) return "";
        char[] s = new char[n];
        for (int i = 0; i < n; i++) s[i] = c;
        return new String(s);
    }

    static String formatDuration(Duration d) {
        long s = d.getStandardSeconds();
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

}
