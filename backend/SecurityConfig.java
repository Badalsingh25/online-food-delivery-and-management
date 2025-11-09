package com.hungerexpress.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import com.hungerexpress.security.JwtAuthFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                
                // Static resources (images, uploads)
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/media/**").permitAll()
                
                // Restaurant & menu APIs (public)
                .requestMatchers(HttpMethod.GET, "/api/restaurants/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/coupons/**").permitAll()
                
                // Cart APIs (public for guest carts)
                .requestMatchers("/api/cart/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/cart/items").permitAll()
                
                // Orders - Customer endpoints
                .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()          // create is public
                .requestMatchers(HttpMethod.GET, "/api/orders/*").permitAll()          // view single is public (used by track page)
                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/cancel").permitAll() // cancel link is public
                // List current user's orders requires authentication (JWT needed to resolve userId)
                .requestMatchers(HttpMethod.GET, "/api/orders").authenticated()
                
                // Payment webhook
                .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                
                // Admin endpoints (require ADMIN role)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Owner endpoints (require OWNER role)
                .requestMatchers("/api/owner/**").hasRole("OWNER")
                
                // Agent specific endpoints (require AGENT role)
                .requestMatchers("/api/orders/agent/**").hasRole("AGENT")
                .requestMatchers("/api/agent/**").hasRole("AGENT")
                
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:4000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
