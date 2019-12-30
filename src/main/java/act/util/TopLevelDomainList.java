package act.util;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2019 ActFramework
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

import act.Act;
import act.app.event.SysEventId;
import act.inject.util.LoadResource;
import act.job.Cron;
import act.job.OnAppStart;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.osgl.util.IO;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Download top level domain list from http://data.iana.org/TLD/tlds-alpha-by-domain.txt.
 *
 * If it cannot be download then use the built in one in resource/tld.list
 */
@Singleton
public class TopLevelDomainList extends LogSupport {

    public static final String CRON_TLD_RELOAD = "cron.act.tld-reload";

    @LoadResource("tld.list")
    private List<String> list;
    private Set<String> quickLookup;

    public TopLevelDomainList() {
    }

    /**
     * Check if a string is top level domain
     * @param s the string to be checked
     * @return `true` if `s` is top level domain or `false` otherwise
     */
    public boolean isTld(String s) {
        return quickLookup.contains(s.toUpperCase());
    }

    /**
     * Strip the first comment line from the tld.list and build quick lookup
     */
    @PostConstruct
    public void filter() {
        doFilter();
    }

    private void doFilter() {
        if (list.get(0).startsWith("#")) {
            list.remove(0);
        }
        quickLookup = new HashSet<>(list);
    }

    /**
     * Reload list from http://data.iana.org/TLD/tlds-alpha-by-domain.txt
     *
     * This method is scheduled to be executed at the time setup in `cron.act.tld-reload`
     * the default value is 0 2 * * * *, i.e. 2am every day.
     */
    @Cron(CRON_TLD_RELOAD)
    public void refresh() {
        if (!Act.app().isStarted()) {
            // in very rare case that cron job running while app is hot-reloading
            Act.app().jobManager().on(SysEventId.POST_STARTED, new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            });
            return;
        }
        try {
            downloadTld();
        } catch (UnknownHostException e) {
            // ignore - refer to https://github.com/actframework/actframework/issues/1265
        } catch (Exception e) {
            warn(e, "error download TLD list file");
        }
    }

    private void downloadTld() throws Exception {
        OkHttpClient http = new OkHttpClient.Builder().build();
        Request req = new Request.Builder().url("http://data.iana.org/TLD/tlds-alpha-by-domain.txt").get().build();
        Response resp = http.newCall(req).execute();
        List<String> newList = IO.read(resp.body().charStream()).toLines();
        resp.close();
        list = newList;
        filter();
    }

    public static void main(String[] args) throws Exception {
        TopLevelDomainList list = new TopLevelDomainList();
        list.downloadTld();
        System.out.println(list.list);
    }

}
