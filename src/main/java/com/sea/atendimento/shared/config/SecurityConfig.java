package com.sea.atendimento.shared.config;

import com.sea.atendimento.shared.security.JwtAuthenticationFilter;
import com.sea.atendimento.shared.security.SecurityExceptionDelegate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_ROUTES = {
            "/auth/**",
            "/error",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityExceptionDelegate securityExceptionDelegate;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SecurityExceptionDelegate securityExceptionDelegate) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityExceptionDelegate = securityExceptionDelegate;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/analyst/**").hasAnyRole("ANALYST", "ADMIN")
                        .requestMatchers("/solicitations/**").hasRole("CLIENT")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityExceptionDelegate)
                        .accessDeniedHandler(securityExceptionDelegate))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}