package com.smartit.smartsales.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:4200",
                "http://localhost:[*]",
                "http://127.0.0.1:[*]"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/api/auth/**").permitAll()

                // Assistant intelligent : tous les rôles connectés
                .requestMatchers("/api/assistant/**").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")

                // Notifications intelligentes : tous les rôles connectés
                .requestMatchers("/api/notifications/**").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")

                // Agrégats BI tableau de bord : MANAGER et ADMIN uniquement
                .requestMatchers("/api/dashboard/**").hasAnyRole("MANAGER", "ADMIN")

                // ADMIN uniquement
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // MANAGER + ADMIN : écriture performances, gestion clients/zones/compétences
                .requestMatchers(HttpMethod.POST, "/api/performances/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/performances/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/performances/**").hasAnyRole("MANAGER", "ADMIN")
                // COMMERCIAL peut enregistrer sa présence (checkin/checkout sur ses propres visites)
                .requestMatchers(HttpMethod.POST, "/api/visites/*/checkin").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/visites/*/checkout").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/visites/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/visites/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/commerciaux/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/commerciaux/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/zones/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/competences/**").hasAnyRole("MANAGER", "ADMIN")

                // COMMERCIAL et au-dessus : consultation + planning propre
                .requestMatchers(HttpMethod.GET, "/api/performances/calculees/me").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/performances/calculees").hasAnyRole("MANAGER", "ADMIN")
                // Export CSV et analyse globale : MANAGER/ADMIN uniquement
                .requestMatchers(HttpMethod.GET, "/api/performances/rapport").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/performances/analyse").hasAnyRole("MANAGER", "ADMIN")
                // Analyse personnelle : accessible au COMMERCIAL lui-même
                .requestMatchers(HttpMethod.GET, "/api/performances/analyse/me").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/performances/**").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/visites/**").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/visites/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/visites/**").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/commerciaux/**").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/clients/**").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/commerciaux/**").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")
                .requestMatchers("/api/clients/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/planning/**").hasAnyRole("COMMERCIAL", "MANAGER", "ADMIN")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
