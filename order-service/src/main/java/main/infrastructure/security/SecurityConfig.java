package main.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/admin/**").hasRole(SecurityRoles.ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/orders/stock", "/api/orders/custom")
                        .hasAnyRole(SecurityRoles.USER, SecurityRoles.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/orders/stock", "/api/orders/stock/**", "/api/orders/custom", "/api/orders/custom/**")
                        .hasAnyRole(SecurityRoles.USER, SecurityRoles.MANAGER, SecurityRoles.ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/stock/**")
                        .hasAnyRole(SecurityRoles.USER, SecurityRoles.MANAGER, SecurityRoles.ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/custom/**")
                        .hasAnyRole(SecurityRoles.USER, SecurityRoles.MANAGER, SecurityRoles.ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/test-drives/requests")
                        .hasAnyRole(SecurityRoles.USER, SecurityRoles.ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/test-drives/requests")
                        .hasAnyRole(SecurityRoles.MANAGER, SecurityRoles.ADMIN)
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            Converter<Jwt, java.util.Collection<org.springframework.security.core.GrantedAuthority>> realmRoleConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(realmRoleConverter);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri));
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
