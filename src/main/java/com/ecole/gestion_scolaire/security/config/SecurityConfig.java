package com.ecole.gestion_scolaire.security.config;

import com.ecole.gestion_scolaire.security.filter.PasswordChangeRequiredFilter;
import com.ecole.gestion_scolaire.security.handler.RestAccessDeniedHandler;
import com.ecole.gestion_scolaire.security.handler.RestAuthenticationEntryPoint;
import com.ecole.gestion_scolaire.security.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            PasswordChangeRequiredFilter passwordChangeRequiredFilter
    ) {
        this.customUserDetailsService = customUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.passwordChangeRequiredFilter = passwordChangeRequiredFilter;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(customUserDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            DaoAuthenticationProvider authenticationProvider
    ) throws Exception {

        CookieCsrfTokenRepository csrfTokenRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        csrfTokenRepository.setCookiePath("/");

        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler =
                new CsrfTokenRequestAttributeHandler();

        http
                .authenticationProvider(authenticationProvider)

                .securityContext(securityContext ->
                        securityContext
                                .securityContextRepository(
                                        securityContextRepository
                                )
                                .requireExplicitSave(true)
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                .csrf(csrf ->
                        csrf
                                .csrfTokenRepository(csrfTokenRepository)
                                .csrfTokenRequestHandler(csrfTokenRequestHandler)
                                .ignoringRequestMatchers(
                                        "/api/auth/login"
                                )
                )

                .addFilterAfter(
                        new CsrfCookieFilter(),
                        CsrfFilter.class
                )

                .addFilterAfter(
                        passwordChangeRequiredFilter,
                        SecurityContextHolderFilter.class
                )

                .exceptionHandling(exceptions ->
                        exceptions
                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                )

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/api/auth/login"
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }
}