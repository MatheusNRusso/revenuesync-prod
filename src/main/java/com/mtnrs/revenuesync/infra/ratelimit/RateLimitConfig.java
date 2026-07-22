package com.mtnrs.revenuesync.infra.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitConfig {

    private boolean enabled = true;
    private Public publicEndpoints = new Public();
    private Auth authEndpoints = new Auth();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Public getPublicEndpoints() { return publicEndpoints; }
    public void setPublicEndpoints(Public publicEndpoints) { this.publicEndpoints = publicEndpoints; }

    public Auth getAuthEndpoints() { return authEndpoints; }
    public void setAuthEndpoints(Auth authEndpoints) { this.authEndpoints = authEndpoints; }

    public static class Public {
        private int capacity = 100;
        private int refillTokens = 100;
        private String refillPeriod = "1m";

        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }

        public int getRefillTokens() { return refillTokens; }
        public void setRefillTokens(int refillTokens) { this.refillTokens = refillTokens; }

        public String getRefillPeriod() { return refillPeriod; }
        public void setRefillPeriod(String refillPeriod) { this.refillPeriod = refillPeriod; }
    }

    public static class Auth {
        private int capacity = 5;
        private int refillTokens = 5;
        private String refillPeriod = "1m";

        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }

        public int getRefillTokens() { return refillTokens; }
        public void setRefillTokens(int refillTokens) { this.refillTokens = refillTokens; }

        public String getRefillPeriod() { return refillPeriod; }
        public void setRefillPeriod(String refillPeriod) { this.refillPeriod = refillPeriod; }
    }
}