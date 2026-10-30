package com.livepoll.backendsrc.API.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private Policy general = new Policy(60, 60, Duration.ofMinutes(1));
    private Policy auth = new Policy(10, 10, Duration.ofMinutes(1));
    private Policy vote = new Policy(30, 30, Duration.ofMinutes(1));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Policy getGeneral() {
        return general;
    }

    public void setGeneral(Policy general) {
        this.general = general;
    }

    public Policy getAuth() {
        return auth;
    }

    public void setAuth(Policy auth) {
        this.auth = auth;
    }

    public Policy getVote() {
        return vote;
    }

    public void setVote(Policy vote) {
        this.vote = vote;
    }

    public static class Policy {
        private long capacity;
        private long refillTokens;
        private Duration refillDuration;

        public Policy() {
        }

        public Policy(long capacity, long refillTokens, Duration refillDuration) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillDuration = refillDuration;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(long refillTokens) {
            this.refillTokens = refillTokens;
        }

        public Duration getRefillDuration() {
            return refillDuration;
        }

        public void setRefillDuration(Duration refillDuration) {
            this.refillDuration = refillDuration;
        }
    }
}
