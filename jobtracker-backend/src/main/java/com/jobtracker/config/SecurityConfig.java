package com.jobtracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.config.Customizer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.jwtFilter = jwtFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // SECURITY FIX: Add security headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny()) // Prevent clickjacking
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)) // XSS protection
                .contentTypeOptions(Customizer.withDefaults()) // Already enabled by default
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/test/**").permitAll()
                .requestMatchers("/feedback").permitAll() // Allow anonymous feedback submission
                .requestMatchers("/login/**", "/oauth2/**").permitAll() // OAuth2 endpoints
                // SECURITY FIX: Protect sensitive endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll() // Only allow health/info
                .requestMatchers("/actuator/**").authenticated() // Require auth for other actuator endpoints
                .requestMatchers("/reports/**").authenticated() // Require auth for reports
                .requestMatchers("/h2-console/**").denyAll() // Disable H2 console (use profiles for dev)
                // Protected endpoints
                .requestMatchers("/applications/**").authenticated()
                .requestMatchers("/jobs/**").authenticated()
                .requestMatchers("/files/**").authenticated()
                .requestMatchers("/notifications/**").authenticated()
                .requestMatchers("/feedback/**").authenticated() // Other feedback endpoints require auth
                .anyRequest().authenticated() // SECURITY FIX: Deny by default instead of permitAll
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use specific origins from configuration instead of wildcard
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setMaxAge(3600L); // Cache preflight requests for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}