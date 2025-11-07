package com.yourcompany.multitenant.config;

import com.yourcompany.multitenant.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' http: https: data: blob:;")))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // --- Public resources ---
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/favicon.ico"),
                                new AntPathRequestMatcher("/login.html"),
                                new AntPathRequestMatcher("/register.html"),
                                new AntPathRequestMatcher("/index.html"),
                                new AntPathRequestMatcher("/static/**"),
                                new AntPathRequestMatcher("/public/**"),
                                new AntPathRequestMatcher("/**/*.js"),
                                new AntPathRequestMatcher("/**/*.css"),
                                new AntPathRequestMatcher("/**/*.html")
                        ).permitAll()

                        .requestMatchers(
                                new AntPathRequestMatcher("/sso/jwt/**"),
                                new AntPathRequestMatcher("/sso/saml/**"),
                                new AntPathRequestMatcher("/sso/oauth/**")
                        ).permitAll()

                        // --- Auth APIs ---
                        .requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/sso/providers")).permitAll()

                        // --- SSO Routes ---
                        .requestMatchers(
                                new AntPathRequestMatcher("/sso/jwt/**"),
                                new AntPathRequestMatcher("/sso/saml/**"),
                                new AntPathRequestMatcher("/sso/oauth/**"),
                                new AntPathRequestMatcher("/api/sso/**")
                        ).permitAll()

                        .requestMatchers(new AntPathRequestMatcher("/api/super-admin/**")).hasAuthority("SUPER_ADMIN")


                        // --- Dashboards ---
                        .requestMatchers(new AntPathRequestMatcher("/super-admin-dashboard.html"))
                        .hasAuthority("SUPER_ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/customer-admin-dashboard.html"))
                        .hasAnyAuthority("SUPER_ADMIN", "CUSTOMER_ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/end-user-dashboard.html"))
                        .hasAnyAuthority("SUPER_ADMIN", "CUSTOMER_ADMIN", "END_USER")

                        // --- Role-specific REST APIs ---
                        .requestMatchers(new AntPathRequestMatcher("/api/super-admin/**"))
                        .hasAuthority("SUPER_ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/customer-admin/**"))
                        .hasAnyAuthority("SUPER_ADMIN", "CUSTOMER_ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/end-user/**"))
                        .hasAnyAuthority("SUPER_ADMIN", "CUSTOMER_ADMIN", "END_USER")

                        // --- Everything else requires authentication ---
                        .anyRequest().authenticated()
                )
                .authenticationProvider(daoAuthenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
