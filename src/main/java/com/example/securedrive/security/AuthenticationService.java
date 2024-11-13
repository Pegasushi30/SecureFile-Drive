package com.example.securedrive.security;

import com.example.securedrive.dto.*;
import com.example.securedrive.model.Role;
import com.example.securedrive.model.User;
import com.example.securedrive.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthenticationService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtService jwtService,
                                 AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public RegisterResponse register(RegisterRequest request) throws NoSuchAlgorithmException {
        // AES-256 Key oluşturma
        String aesKey = generateAESKey();

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEncryptionKey(aesKey);  // AES anahtarı kullanıcıya atanıyor

        User savedUser = userRepository.save(user);

        // JWT tokenleri oluşturma
        Map<String, Object> claims = Map.of(
                "roles", savedUser.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList())
        );

        String accessToken = jwtService.generateAccessToken(claims, savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return new RegisterResponse(accessToken, refreshToken, savedUser.getId(), aesKey);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> claims = Map.of(
                "roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList())
        );

        String accessToken = jwtService.generateAccessToken(claims, user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthenticationResponse(accessToken, refreshToken, user.getId());
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        if (jwtService.isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh token expired");
        }

        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> claims = Map.of(
                "roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList())
        );

        String newAccessToken = jwtService.generateAccessToken(claims, user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return new AuthenticationResponse(newAccessToken, newRefreshToken, user.getId());
    }

    private String generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);  // 256 bitlik anahtar oluşturma
        byte[] key = keyGen.generateKey().getEncoded();
        return Base64.getEncoder().encodeToString(key);
    }
}

