package com.livepoll.backendsrc.API.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfiguration {

    private final BearerTokenAuthFilter bearerTokenAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    @Value("${app.cors.allowed-origin:http://localhost:5173}")
    private String allowedOrigin;

    public SecurityConfiguration(BearerTokenAuthFilter bearerTokenAuthFilter, RateLimitFilter rateLimitFilter) {
        this.bearerTokenAuthFilter = bearerTokenAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {
                })
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/error", "/", "/index.html", "/assets/**", "/favicon.ico").permitAll()
                        .requestMatchers("/api/user/auth/register").permitAll()
                        .requestMatchers("/api/user/auth/login").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/polls/get-polls").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/polls/saved/{pollId}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/polls/{roomCode}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/polls/{roomCode}/details").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/polls/{roomCode}/vote").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/polls").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/polls/{pollId}/start").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/polls/{pollId}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/polls/{pollId}").authenticated()
                        .requestMatchers("/api/user/auth").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(bearerTokenAuthFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://[::1]:*",
                allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }
}
