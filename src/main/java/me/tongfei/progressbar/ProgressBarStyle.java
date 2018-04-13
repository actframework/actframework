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

/**
 * Represents the display style of a progress bar.
 * @author Tongfei Chen
 * @since 0.5.1
 */
public enum ProgressBarStyle {

    /** Use Unicode block characters to draw the progress bar. */
    UNICODE_BLOCK("│", "│", '█', ' ', " ▏▎▍▌▋▊▉"),

    /** Use only ASCII characters to draw the progress bar. */
    ASCII("[", "]", '=', ' ', ">");

    String leftBracket;
    String rightBracket;
    char block;
    char space;
    String fractionSymbols;

    ProgressBarStyle(String leftBracket, String rightBracket, char block, char space, String fractionSymbols) {
        this.leftBracket = leftBracket;
        this.rightBracket = rightBracket;
        this.block = block;
        this.space = space;
        this.fractionSymbols = fractionSymbols;
    }

}
