package com.hungerexpress.auth;

import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenRepository refreshTokens;

    @Transactional
    public AuthResponse signup(SignupRequest req){
        if (users.existsByEmail(req.email())) throw new IllegalArgumentException("Email already registered");
        String role = req.role() == null ? "CUSTOMER" : req.role();
        User u = User.builder()
                .email(req.email())
                .password(encoder.encode(req.password()))
                .fullName(req.fullName())
                .enabled(true)
                .role(role)
                .build();
        users.save(u);
        String token = jwt.generate(Map.of("role", role, "uid", u.getId()), u.getEmail());
        String rt = issueRefreshToken(u);
        return new AuthResponse(token, rt, u.getEmail(), role, u.getFullName());
    }

    @Transactional
    public AuthResponse login(AuthRequest req){
        User u = users.findByEmail(req.email()).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!encoder.matches(req.password(), u.getPassword())) throw new IllegalArgumentException("Invalid credentials");
        String role = u.getRole() != null ? u.getRole() : "CUSTOMER";
        String token = jwt.generate(Map.of("role", role, "uid", u.getId()), u.getEmail());
        String rt = issueRefreshToken(u);
        return new AuthResponse(token, rt, u.getEmail(), role, u.getFullName());
    }

    @Transactional(readOnly = true)
    public ProfileResponse me(String token){
        String email = jwt.extractUsername(token);
        User u = users.findByEmail(email).orElseThrow();
        String role = u.getRole() != null ? u.getRole() : "CUSTOMER";
        return new ProfileResponse(u.getId(), u.getEmail(), u.getFullName(), role);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken){
        RefreshToken rt = refreshTokens.findByToken(refreshToken).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (rt.isRevoked() || rt.getExpiresAt().isBefore(java.time.Instant.now())){
            throw new IllegalArgumentException("Invalid refresh token");
        }
        User u = rt.getUser();
        String role = u.getRole() != null ? u.getRole() : "CUSTOMER";
        String newAccess = jwt.generate(Map.of("role", role, "uid", u.getId()), u.getEmail());
        // rotate: revoke old and issue new
        rt.setRevoked(true);
        refreshTokens.save(rt);
        String newRt = issueRefreshToken(u);
        return new AuthResponse(newAccess, newRt, u.getEmail(), role, u.getFullName());
    }

    private String issueRefreshToken(User user){
        String token = java.util.UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(java.time.Instant.now().plus(java.time.Duration.ofDays(30)))
                .revoked(false)
                .build();
        refreshTokens.save(rt);
        return token;
    }
}
