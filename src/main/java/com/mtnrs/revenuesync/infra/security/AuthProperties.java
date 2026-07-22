package com.mtnrs.revenuesync.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /**
     * When false (default / production), direct email+password registration
     * is disabled and the platform is GitHub-first: accounts are only created
     * via OAuth. Kept as a flag so a local demo profile can re-enable it.
     */
    private boolean allowDirectRegistration = false;

    public boolean isAllowDirectRegistration() {
        return allowDirectRegistration;
    }

    public void setAllowDirectRegistration(boolean allowDirectRegistration) {
        this.allowDirectRegistration = allowDirectRegistration;
    }
}