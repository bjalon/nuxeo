/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.lib.stream.log;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Extends LogLag with lower and upper timestamps to express lag as a latency.
 *
 * @since 10.1
 */
public class Latency {
    protected final LogLag lag;

    protected long lower;

    protected long upper;

    public Latency(long lower, long upper, LogLag lag) {
        Objects.requireNonNull(lag);
        this.lower = lower;
        this.upper = upper;
        this.lag = lag;
    }

    public static Latency noLatency(long upper, LogLag lag) {
        Objects.requireNonNull(lag);
        if (lag.lag() != 0) {
            throw new IllegalArgumentException("Lag found: " + lag);
        }
        return new Latency(0, upper, lag);
    }

    public static Latency of(List<Latency> latencies) {
        LogLag lag = LogLag.of(latencies.stream().map(Latency::lag).collect(Collectors.toList()));
        final long[] start = { Long.MAX_VALUE };
        final long[] end = { 0 };
        latencies.forEach(item -> {
            if (item.lower > 0) {
                start[0] = min(start[0], item.lower);
            }
            end[0] = max(end[0], item.upper);
        });
        return new Latency(start[0] == Long.MAX_VALUE ? 0 : start[0], end[0], lag);
    }

    /**
     * Returns the latency expressed in millisecond.
     */
    public long latency() {
        return lag.lag() > 0 ? upper - lower : 0;
    }

    /**
     * Returns the lower timestamp.
     */
    public long lower() {
        return lower;
    }

    /**
     * Returns the upper timestamp.
     */
    public long upper() {
        return upper;
    }

    public LogLag lag() {
        return lag;
    }

    @Override
    public String toString() {
        return "Latency{" + "lat=" + latency() + ", lower=" + lower + ", upper=" + upper + ", lag=" + lag + '}';
    }

}