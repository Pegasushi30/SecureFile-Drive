package com.example.securedrive.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final B2CConfiguration b2cConfiguration;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOidcUserService customOidcUserService;

    public SecurityConfig(B2CConfiguration b2cConfiguration,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomOidcUserService customOidcUserService) {
        this.b2cConfiguration = b2cConfiguration;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/home", "/upload", "/files", "/css/**", "/js/**", "/favicon.ico", "/password-reset-callback").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .clientRegistrationRepository(clientRegistrationRepository())
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error=true")
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("https://sdfile.b2clogin.com/sdfile.onmicrosoft.com/B2C_1_SignUpSignIn/oauth2/v2.0/logout" +
                                "?post_logout_redirect_uri=https://localhost:8443/")
                        .permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration b2cRegistration = ClientRegistration.withRegistrationId("azureb2c")
                .clientId(b2cConfiguration.getClientId())
                .clientSecret(b2cConfiguration.getClientSecret())
                .scope("openid", "profile", "email", "https://sdfile.onmicrosoft.com/6c4e0c80-e9f5-4922-a3d3-097549f658d2/myapi.read")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(b2cConfiguration.getRedirectUri())
                .authorizationUri(b2cConfiguration.getAuthorizationUri())
                .tokenUri(b2cConfiguration.getTokenUri())
                .jwkSetUri(b2cConfiguration.getJwkSetUri())
                .userNameAttributeName("sub")
                .clientName("Azure B2C")
                .build();

        return new InMemoryClientRegistrationRepository(b2cRegistration);
    }

}
