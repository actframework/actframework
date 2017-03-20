package playground;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
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

import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.N;
import org.osgl.util.S;

import java.util.ArrayList;
import java.util.List;

public class ContentSuffixSensor {
    private static String foo(String url) {
        if (url.endsWith(".json") || url.endsWith("/json")) {
            url = url.substring(0, url.length() - 5);
        } else if (url.endsWith(".xml") || url.endsWith("/xml")) {
            url = url.substring(0, url.length() - 4);
        } else if (url.endsWith(".csv") || url.endsWith("/csv")) {
            url = url.substring(0, url.length() - 4);
        }
        return url;
    }

    private static int bare(String url) {
        return url.length();
    }

    private static final char[] json = {'j', 's', 'o'};
    private static final char[] xml = {'x', 'm'};
    private static final char[] csv = {'c', 's'};

    private static String bar(String url) {
        int sz = url.length();
        int start = sz - 1;
        char c = url.charAt(start);
        char[] trait;
        int sepPos = 3;
        switch (c) {
            case 'n':
                sepPos = 4;
                trait = json;
                break;
            case 'l':
                trait = xml;
                break;
            case 'v':
                trait = csv;
                break;
            default:
                return url;
        }
        char sep = url.charAt(start - sepPos);
        if (sep != '.' && sep != '/') {
            return url;
        }
        for (int i = 1; i < sepPos; ++i) {
            if (url.charAt(start - i) != trait[sepPos - i - 1]) {
                return url;
            }
        }
        return url.substring(0, sz - sepPos - 1);
    }

    private static long benchmarkFoo(List<String> urls, int times) {
        long ms = $.ms();
        for (int i = 0; i < times; ++i) {
            for (String url : urls) {
                foo(url);
            }
        }
        return $.ms() - ms;
    }

    private static long benchmarkBar(List<String> urls, int times) {
        long ms = $.ms();
        for (int i = 0; i < times; ++i) {
            for (String url : urls) {
                bar(url);
            }
        }
        return $.ms() - ms;
    }

    private static long benchmarkBare(List<String> urls, int times) {
        long ms = $.ms();
        for (int i = 0; i < times; ++i) {
            for (String url : urls) {
                bare(url);
            }
        }
        return $.ms() - ms;
    }

    private static List<String> prepareUrlList() {
        final int size = 100;
        List<String> suffixes = C.listOf(".json,/json,.xml,/xml,.csv,/csv".split(","));
        if (null == generated) {
            generated = new ArrayList<String>(size);
            for (int i = 0; i < size; ++i) {
                String url = S.random(30 + N.randInt(40));
                if (i % 2 == 0) {
                    url += $.random(suffixes);
                }
                generated.add(url);
            }
        }
        return generated;
    }

    private static List<String> generated;

    private static void warmUp() {
        final int times = 1000 * 1000;
        List<String> urls = prepareUrlList();
        benchmarkFoo(urls, times);
        benchmarkBar(urls, times);
        benchmarkBare(urls, times);
    }

    private static void benchmark() {
        final int times = 1000 * 1000;
        List<String> urls = prepareUrlList();
        long l = benchmarkBare(urls, times);
        System.out.println(l);
        l = benchmarkBar(urls, times);
        System.out.println(l);
        l = benchmarkFoo(urls, times);
        System.out.println(l);
    }

    private static void verify() {
        String url = "http://abc.com/xyz.ddd/key.json";
        System.out.println(foo(url));
        System.out.println(bar(url));

        url = "http://abc/123.jSon";
        System.out.println(foo(url));
        System.out.println(bar(url));

        url = "http://abc/123.csv";
        System.out.println(foo(url));
        System.out.println(bar(url));
    }

    public static void main(String[] args) {
        verify();
        warmUp();
        benchmark();
    }
}
