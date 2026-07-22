package com.mtnrs.revenuesync.infra.security;

import com.mtnrs.revenuesync.infra.ratelimit.RateLimitFilter;
import com.mtnrs.revenuesync.repository.UserRepository;
import com.mtnrs.revenuesync.service.GitHubOAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.PermissionsPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserRepository userRepository;
    private final GitHubOAuthService gitHubOAuthService;
    private final PasswordEncoder passwordEncoder;
    private final String frontendUrl;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          UserRepository userRepository,
                          GitHubOAuthService gitHubOAuthService,
                          PasswordEncoder passwordEncoder,
                          @org.springframework.beans.factory.annotation.Value("${app.frontend-url}") String frontendUrl) {
        this.jwtAuthFilter  = jwtAuthFilter;
        this.userRepository = userRepository;
        this.gitHubOAuthService = gitHubOAuthService;
        this.passwordEncoder = passwordEncoder;
        this.frontendUrl    = frontendUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'")
                        )
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(content -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        .addHeaderWriter(new PermissionsPolicyHeaderWriter("camera=(), microphone=(), geolocation=()"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/validate").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/public/merchants/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/profiles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/solana/status/**").permitAll()
                        .requestMatchers("/api/solana/transaction-request/**").permitAll()
                        .requestMatchers("/api/solana/payment/**").authenticated()
                        .requestMatchers("/api/payments/**", "/api/conversions/**").hasRole("ADMIN")
                        .requestMatchers("/api/me/**").authenticated()
                        .requestMatchers("/api/discover/**", "/api/public/pay/**").authenticated()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/chats/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/oauth2/authorization")
                        )
                        .successHandler((request, response, authentication) -> {
                            String jwt = gitHubOAuthService.handleOAuthSuccess(authentication);
                            String redirectUrl = frontendUrl + "/oauth2/callback?token=" + jwt;
                            response.setContentType("text/html");
                            response.getWriter().write(
                                    "<html><body><script>window.location.href='" + redirectUrl + "'</script></body></html>"
                            );
                        })
                        .failureHandler((request, response, exception) -> {
                            response.sendRedirect(frontendUrl + "/login?error=oauth2");
                        })
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                        })
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(email ->
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"))
        );
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}