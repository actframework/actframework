package act.session;

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

import act.app.App;
import act.conf.AppConfig;
import org.joda.time.DateTime;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Singleton
public class RotationSecretProvider {

    private String rawSecret;
    private String lastSecret;
    private String curSecret;
    private String nextSecret;
    private int periodInMinutes;
    private boolean rotateEnabled;

    @Inject
    public RotationSecretProvider(App app) {
        AppConfig<?> config = app.config();
        rawSecret = config.secret();
        rotateEnabled = config.rotateSecret();
        if (rotateEnabled) {
            int period = config.secretRotatePeriod();
            app.jobManager().every("secret-rotator", new Runnable() {
                @Override
                public void run() {
                    rotateSecret();
                }
            }, period, TimeUnit.MINUTES);
            periodInMinutes = period;
            rotateSecret();
        }
    }

    public boolean isRotateEnabled() {
        return rotateEnabled;
    }

    public String rawSecret() {
        return rawSecret;
    }

    public String curSecret() {
        ensureRotateEnabled();
        return curSecret;
    }

    public String lastSecret() {
        ensureRotateEnabled();
        return lastSecret;
    }

    public String nextSecret() {
        ensureRotateEnabled();
        return nextSecret;
    }

    private static final int[] VALID_MINUTES = {
            1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60
    };

    private static final int[] VALID_HOURS = {
            1, 2, 3, 4, 6, 8, 12, 24
    };

    public static int roundToPeriod(int minutes) {
        E.illegalArgumentIf(minutes <= 0);
        if (minutes > 60 * 24) {
            return 60 * 24;
        } else if (minutes > 60) {
            float f = minutes / 60f;
            int hours = Math.round(f);
            int pos = Arrays.binarySearch(VALID_HOURS, hours);
            return (pos >= 0 ? hours : VALID_HOURS[-(pos + 1)]) * 60;
        } if (minutes > 30) {
            int reminder = minutes % 30;
            return 0 == reminder ? minutes : minutes + (reminder < 15 ? -reminder : (30 - reminder));
        } else {
            int pos = Arrays.binarySearch(VALID_MINUTES, minutes);
            return pos >= 0 ? minutes : VALID_MINUTES[-(pos + 1)];
        }
    }

    private void rotateSecret() {
        ensureRotateEnabled();
        lastSecret = curSecret;
        curSecret = null != nextSecret ? nextSecret : calculateCurrentSecret();
        nextSecret = calculateNextSecret();
        if (null == lastSecret) {
            lastSecret = curSecret;
        }
    }

    private String calculateCurrentSecret() {
        return rawSecret + extension(0);
    }

    private String calculateNextSecret() {
        return rawSecret + extension(1);
    }

    private String extension(int forwardStep) {
        long trait;
        if (periodInMinutes > 60) {
            int periodInHours = periodInMinutes / 60;
            int hoursOfDay = DateTime.now().hourOfDay().get();
            int period = hoursOfDay / periodInHours;
            trait = currentHourStart().getMillis() / 1000 / 60;
            if (0 < period) {
                trait += (period * 60);
            }
        } else {
            int minutesOfHour = DateTime.now().minuteOfHour().get();
            int period = minutesOfHour / periodInMinutes;
            trait = currentHourStart().getMillis() / 1000 / 60;
            if (0 < period) {
                trait += period;
            }
        }
        if (forwardStep > 0) {
            trait += forwardStep * periodInMinutes;
        }
        return S.string(trait);
    }

    private void ensureRotateEnabled() {
        E.illegalStateIfNot(rotateEnabled, "secret rotate not enabled");
    }


    private static DateTime currentHourStart() {
        return DateTime.now().withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
    }

}
