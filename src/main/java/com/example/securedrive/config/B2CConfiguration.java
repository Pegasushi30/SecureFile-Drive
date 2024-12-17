package com.example.securedrive.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "b2c")
public class B2CConfiguration {

    private String clientId;
    private String clientSecret;
    private String tenantDomain;
    private String tenantId;
    private String signUpOrSignInPolicy;
    private String passwordResetPolicy;
    private String redirectUri;
    private String authorizationUri;
    private String tokenUri;
    private String jwkSetUri;

    public String getAuthorizationUri() {
        return authorizationUri.replace("<policy-name>", signUpOrSignInPolicy);
    }

    public String getTokenUri() {
        return tokenUri.replace("<policy-name>", signUpOrSignInPolicy);
    }

    public String getJwkSetUri() {
        return jwkSetUri.replace("<policy-name>", signUpOrSignInPolicy);
    }

    public String getPasswordResetAuthorizationUri() {
        return authorizationUri.replace("<policy-name>", passwordResetPolicy);
    }

    public String getPasswordResetTokenUri() {
        return tokenUri.replace("<policy-name>", passwordResetPolicy);
    }
}
