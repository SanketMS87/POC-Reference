package com.rukamini.mail;

import com.rukamini.mail.configuration.SecurityProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SpringSecurityConfig {

    private static final String ROLE_READ = "Read";
    private static final String ROLE_WRITE = "Write";
    private static final String ROLE_OL_EVERYONE = "Everyone";

    public static final List<String> USER_ROLES =
            Arrays.asList(
                    "EMEAPROPUW",
                    "EMEACASUW",
                    "EMEAMARINEUW",
                    "EMEAENGUW",
                    "APACPROPUW",
                    "APACCASUW",
                    "APACMARINEUW",
                    "APACENGUW",
                    "LATAMPROPUW",
                    "LATAMCASUW",
                    "LATAMMARINEUW",
                    "LATAMENGUW",
                    "EMEAPROPUA",
                    "EMEACASUA",
                    "EMEAMARINEUA",
                    "EMEAENGUA",
                    "APACPROPUA",
                    "APACCASUA",
                    "APACMARINEUA",
                    "APACENGUA",
                    "LATAMPROPUA",
                    "LATAMCASUA",
                    "LATAMMARINEUA",
                    "LATAMENGUA",
                    "GLOBALALLPE",
                    "GLOBALALLLEDH",
                    "IT_ADMIN",
                    "IT_ADMIN_BUSINESS",
                    "EMEAALLFD",
                    "APACALLFD",
                    "LATAMALLFD",
                    "DOCUMENTS_ONLY",
                    "READ_ONLY",
                    "HEALTHCARE",
                    "TECH_ACCOUNTANT",
                    "DATA_QUALITY_REVIEWER",
                    "NAHEALTHCARE",
                    "NACONTIGUW",
                    "NAACTCOSTSVCS",
                    "NAPROPUA",
                    "NAPROPUW",
                    "NACASUW",
                    "NACASUA",
                    "NAALLFD",
                    "ADTSPROCESSOR",
                    "CATALOG_MANAGER_EMEA");

    @Bean("allUserRolesWithReadWrite")
    public String[] allUserRolesWithReadWrite() {
        return combineRoles(USER_ROLES, ROLE_READ, ROLE_WRITE);
    }

    @Bean("allUserRoles")
    public String[] allUserRoles() {
        return combineRoles(USER_ROLES);
    }

    @Autowired private Environment environment;

    /**
     * Validates inbound Entra ID bearer tokens (signature via JWKS, issuer and audience). The
     * JWKS fetch is lazy, so no network
     * access happens at startup.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecurityProperties securityProperties) {
        SecurityProperties.Jwt jwtProps = securityProperties.getJwt();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwtProps.getJwkSetUri()).build();
        List<String> allowedAudiences = jwtProps.getAudiences();
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD,
                        aud -> aud != null && !Collections.disjoint(aud, allowedAudiences));
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(jwtProps.getIssuer()),
                        audienceValidator));
        return decoder;
    }

    /**
     * Maps a validated JWT to {@code ROLE_*} authorities.
     *
     * <p>By default only the token's {@code roles} claim is mapped. With {@code
     * app.security.grant-all-roles-to-authenticated=true} every valid token receives all known
     * roles.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            SecurityProperties securityProperties) {
        Converter<Jwt, Collection<GrantedAuthority>> authorities;
        if (securityProperties.isGrantAllRolesToAuthenticated()) {
            List<GrantedAuthority> all = new ArrayList<>();
            for (String role : combineRoles(USER_ROLES, ROLE_READ, ROLE_WRITE, ROLE_OL_EVERYONE)) {
                all.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            authorities = jwt -> all;
        } else {
            JwtGrantedAuthoritiesConverter claimConverter = new JwtGrantedAuthoritiesConverter();
            claimConverter.setAuthoritiesClaimName(securityProperties.getJwt().getRolesClaim());
            claimConverter.setAuthorityPrefix("ROLE_");
            authorities = claimConverter::convert;
        }
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName(securityProperties.getJwt().getUserNameClaim());
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        // Check if 'local' profile is active
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("local")) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            AccessDeniedHandler accessDenied =
                    (request, response, accessDeniedException) -> {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                        response.getWriter()
                                .write(
                                        "Access denied. You do not have the required"
                                                + " permissions to access this resource.");
                    };
            AuthenticationEntryPoint entryPoint =
                    (request, response, authException) -> {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                        response.getWriter()
                                .write(
                                        "Authentication failed."
                                                + " Please provide valid credentials.");
                    };
            http.exceptionHandling(
                    ex ->
                            ex.accessDeniedHandler(accessDenied)
                                    .authenticationEntryPoint(entryPoint));

            http.authorizeHttpRequests(
                            auth ->
                                    auth.requestMatchers(
                                                    "/swagger-ui/**",
                                                    "/swagger-ui.html",
                                                    "/v2/api-docs",
                                                    "/swagger-resources/**",
                                                    "/v3/api-docs/**",
                                                    "/webjars/**",
                                                    "/api/health")
                                            .permitAll()
                                            .anyRequest()
                                            .authenticated())
                    .oauth2ResourceServer(
                            oauth2 ->
                                    oauth2.authenticationEntryPoint(entryPoint)
                                            .accessDeniedHandler(accessDenied)
                                            .jwt(
                                                    jwt ->
                                                            jwt.jwtAuthenticationConverter(
                                                                    jwtAuthenticationConverter)));
        }
        // CSRF is disabled intentionally: this is a stateless REST API authenticated by bearer
        // tokens (Authorization header). No session-cookie-based state-changing form submissions
        // are processed by Spring, so CSRF tokens are not needed.
        http.csrf(
                AbstractHttpConfigurer
                        ::disable); // NOSONAR - safe: bearer-token auth, stateless REST API
        http.sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    private static String[] combineRoles(List<String> baseRoles, String... additionalRoles) {
        List<String> combined = new java.util.ArrayList<>(baseRoles);
        combined.addAll(Arrays.asList(additionalRoles));
        return combined.toArray(new String[0]);
    }
}
