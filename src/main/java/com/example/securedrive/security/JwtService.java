package com.example.securedrive.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtDecoder jwtDecoder;

    public JwtService(@Value("${b2c.issuer-uri}") String issuerUri) {
        this.jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);
    }

    public Jwt decodeToken(String token) throws JwtException {
        return jwtDecoder.decode(token);
    }

    public String extractUsername(String token) {
        Jwt jwt = decodeToken(token);
        return jwt.getClaimAsString("sub");
    }

    public boolean isTokenValid(String token) {
        try {
            decodeToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
