package io.fortalis.fortalisauth.config;

import io.fortalis.fortalisauth.crypto.KeyProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Stateless API security:
 * - Permit JWKS and auth endpoints
 * - Everything else requires authentication (not used yet)
 * - Security headers hardened
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/auth/register", "/auth/login", "/auth/logout", "/auth/refresh", "/auth/login/start", "/auth/login/complete").permitAll()
                        .requestMatchers("/.well-known/**", "/actuator/health").permitAll()
                        .requestMatchers("/auth/mfa/**").authenticated()
                        .anyRequest().authenticated()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'"))
                        .addHeaderWriter((request, response) -> response.setHeader("Permissions-Policy",
                                "geolocation=(), microphone=(), camera=()"))
                );
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(KeyProvider keyProvider, AuthJwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keyProvider.publicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.getIssuer()));
        return decoder;
    }
}
