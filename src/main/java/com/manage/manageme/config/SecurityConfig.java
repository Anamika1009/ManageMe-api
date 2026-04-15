package com.manage.manageme.config;

import com.manage.manageme.security.JwtRequestFilter;
import com.manage.manageme.service.AppUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppUserDetailService appUserDetailService;
    private final JwtRequestFilter jwtRequestFilter;

    @Value("${manageme.frontend.url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // 1. Enable CORS with our custom configuration
                .cors(Customizer.withDefaults())
                // 2. Disable CSRF (since we use JWT and Stateless sessions)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 3. ALLOW OPTIONS: Very important for CORS preflight (PUT/DELETE)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 4. PUBLIC ENDPOINTS: Base routes that don't need a token
                        .requestMatchers("/api/v1.0/status", "/api/v1.0/health", "/api/v1.0/register", "/api/v1.0/activate", "/api/v1.0/login", "/error").permitAll()
                        // 5. PROTECTED ENDPOINTS: Everything else (like /update-profile) needs JWT
                        .anyRequest().authenticated()
                )
                // 6. STATELESS SESSION: No cookies, only JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 7. JWT FILTER: Run this before the default auth filter
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow localhost and your Render frontend URL
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                frontendUrl != null ? frontendUrl : "https://your-frontend-link.vercel.app"
        ));

        // ALLOW ALL METHODS: Critical for PUT (profile updates)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // ALLOW ALL HEADERS: Crucial for Authorization (Bearer Token)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Cache-Control"));

        // Allow sending credentials (like cookies if ever needed, or just standard auth)
        configuration.setAllowCredentials(true);

        // How long the browser should cache this CORS preflight response
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(appUserDetailService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authenticationProvider);
    }
}